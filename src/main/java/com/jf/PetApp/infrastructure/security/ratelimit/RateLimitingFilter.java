package com.jf.PetApp.infrastructure.security.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple in-memory sliding-window rate limiter. Single-instance app, no distributed state
 * needed — a {@code ConcurrentHashMap} keyed by client IP + path is enough; no new dependency
 * (e.g. Bucket4j/Redis) justified for this scale.
 *
 * Deliberately not general-purpose middleware: only the path patterns listed in
 * {@link #RULES} are limited, everything else passes straight through. Two rule groups exist
 * because they guard different things — the auth rule is a tight credential-stuffing/
 * enumeration deterrent, the progression rule is a looser abuse/DoS backstop on endpoints that
 * legitimate app usage already calls repeatedly (e.g. every gamification screen re-evaluates
 * achievements/missions/XP live), so it must not false-positive on normal use.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private record Rule(Predicate<String> pathMatches, int maxRequests, Duration window) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(
                    path -> path.equals("/auth/login")
                            || path.equals("/auth/register")
                            || path.equals("/auth/forgot-password")
                            // Consumes a reset token: unlimited attempts make the token
                            // brute-forceable, and it is the one credential-changing endpoint that
                            // needs no existing session. forgot-password was already limited, so
                            // limiting the request side but not the redemption side left the
                            // cheaper half of the attack open.
                            || path.equals("/auth/reset-password"),
                    5, Duration.ofSeconds(60)),
            new Rule(
                    path -> path.equals("/api/v1/learning/progress")
                            || path.equals("/api/v1/achievements")
                            || path.equals("/api/v1/missions")
                            || path.equals("/api/v1/gamification/summary")
                            || (path.startsWith("/api/v1/learning/lessons/") && path.endsWith("/complete")),
                    60, Duration.ofSeconds(60)));

    private final ConcurrentHashMap<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    // A key never gets pruned again once its client stops sending requests — the per-request
    // prune in isRateLimited only ever touches keys that are actively being hit, so an
    // abandoned key's now-stale, possibly-already-empty Deque sits in requestLog forever,
    // growing unbounded over the app's lifetime. No @Scheduled infra exists anywhere else in
    // this codebase yet, so rather than introduce @EnableScheduling for this alone, cleanup
    // rides along on the request path itself: whichever request happens to land after
    // CLEANUP_INTERVAL has elapsed since the last sweep does one, gated by a CAS so concurrent
    // requests can't all sweep at once.
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);
    private static final Duration MAX_RULE_WINDOW = RULES.stream()
            .map(Rule::window)
            .max(Comparator.naturalOrder())
            .orElse(Duration.ZERO);
    private final AtomicLong lastCleanupNanos = new AtomicLong(System.nanoTime());

    // Comma-separated CIDR ranges (e.g. "10.0.0.0/8") of reverse proxies/load balancers this
    // app trusts to have set X-Forwarded-For honestly. Empty by default (the app isn't yet
    // deployed behind a known topology) — see application.properties' app.security.trusted-proxies
    // doc. An IPv4-only matcher (see TrustedProxyMatcher) is enough for the deployment
    // topologies this property currently needs to describe; IPv6 CIDR ranges are treated as
    // untrusted rather than silently mismatched.
    private final List<TrustedProxyMatcher> trustedProxies;

    public RateLimitingFilter(@Value("${app.security.trusted-proxies:}") String trustedProxiesConfig) {
        this.trustedProxies = Arrays.stream(trustedProxiesConfig.split(","))
                .map(String::trim)
                .filter(range -> !range.isEmpty())
                .map(TrustedProxyMatcher::parse)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws jakarta.servlet.ServletException, IOException {

        maybeCleanupStaleKeys();

        String path = request.getRequestURI();
        Rule rule = RULES.stream().filter(r -> r.pathMatches().test(path)).findFirst().orElse(null);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + ":" + path;
        if (isRateLimited(key, rule)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"detail\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key, Rule rule) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(rule.window());
        Deque<Instant> timestamps = requestLog.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Prune, check, and record must happen as one unit per key, or two concurrent
        // requests for the same client could both read a stale count and both pass — a
        // plain ArrayDeque under a per-key synchronized block is simplest here.
        synchronized (timestamps) {
            while (true) {
                Instant oldest = timestamps.peekFirst();
                if (oldest == null || !oldest.isBefore(windowStart)) {
                    break;
                }
                timestamps.pollFirst();
            }

            if (timestamps.size() >= rule.maxRequests()) {
                return true;
            }

            timestamps.addLast(now);
            return false;
        }
    }

    private String clientIp(HttpServletRequest request) {
        // X-Forwarded-For is only honored when the immediate TCP peer (the last hop before
        // this app — a proxy can only be spoofed by attackers if we trust it blindly) is
        // itself inside a configured trusted-proxy range. Absent that config (the default —
        // no known production topology yet), the raw connection address is always used, so a
        // client can never forge this header to bypass rate limiting.
        String remoteAddr = request.getRemoteAddr();
        boolean fromTrustedProxy = trustedProxies.stream().anyMatch(proxy -> proxy.matches(remoteAddr));
        if (!fromTrustedProxy) {
            return remoteAddr;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }

    /**
     * Sweeps {@link #requestLog} for keys whose window has fully elapsed, at most once per
     * {@link #CLEANUP_INTERVAL}. The CAS on {@link #lastCleanupNanos} means at most one of any
     * concurrently-arriving requests performs the sweep; every other request just proceeds.
     */
    private void maybeCleanupStaleKeys() {
        long nowNanos = System.nanoTime();
        long last = lastCleanupNanos.get();
        if (nowNanos - last < CLEANUP_INTERVAL.toNanos()) {
            return;
        }
        if (!lastCleanupNanos.compareAndSet(last, nowNanos)) {
            return;
        }
        cleanupStaleKeys();
    }

    // Package-private and ungated by CLEANUP_INTERVAL/the CAS above: lets tests trigger a sweep
    // deterministically instead of waiting out real wall-clock minutes or mocking System.nanoTime.
    void cleanupStaleKeys() {
        Instant staleBefore = Instant.now().minus(MAX_RULE_WINDOW);
        requestLog.forEach((key, timestamps) -> {
            synchronized (timestamps) {
                while (true) {
                    Instant oldest = timestamps.peekFirst();
                    if (oldest == null || !oldest.isBefore(staleBefore)) {
                        break;
                    }
                    timestamps.pollFirst();
                }
                if (timestamps.isEmpty()) {
                    // remove(key, value) rather than a plain remove(key): only drop this exact
                    // now-empty Deque instance, so a request that raced in via computeIfAbsent
                    // and added a fresh timestamp to it between our emptiness check and this
                    // call is never silently discarded.
                    requestLog.remove(key, timestamps);
                }
            }
        });
    }

    // Package-private: lets tests assert the map actually shrinks after a sweep.
    int trackedKeyCount() {
        return requestLog.size();
    }

    // Package-private: lets tests seed an arbitrarily-old timestamp directly, since production
    // timestamps only ever come from Instant.now() inside isRateLimited and there's no injected
    // Clock to fast-forward in a test.
    void trackForTesting(String key, Instant timestamp) {
        requestLog.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(timestamp);
    }
}
