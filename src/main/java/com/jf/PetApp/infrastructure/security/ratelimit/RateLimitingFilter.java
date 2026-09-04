package com.jf.PetApp.infrastructure.security.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
}
