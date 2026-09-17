package com.jf.PetApp.infrastructure.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        filter = new RateLimitingFilter("");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    @Test
    void doFilterInternal_PathNotRateLimited_AlwaysPassesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/me");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        for (int i = 0; i < 20; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(20)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilterInternal_UpToLimitRequestsFromSameIp_AllPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilterInternal_ExceedingLimit_Returns429AndStopsChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        // The 6th, rejected call never reaches downstream filters/controllers.
        verify(filterChain, times(5)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DifferentIpsOnSamePath_TrackedIndependently() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/register");

        when(request.getRemoteAddr()).thenReturn("10.0.0.4");
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        filter.doFilterInternal(request, response, filterChain);

        verify(response, never()).setStatus(429);
        verify(filterChain, times(6)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DifferentPathsForSameIp_TrackedIndependently() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.6");

        when(request.getRequestURI()).thenReturn("/auth/login");
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        when(request.getRequestURI()).thenReturn("/auth/forgot-password");
        filter.doFilterInternal(request, response, filterChain);

        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilterInternal_IgnoresXForwardedForWhenNoTrustedProxyIsConfigured() throws Exception {
        // filter (from setUp) has no trusted proxies configured — the default, since this app
        // isn't deployed behind a known reverse proxy yet. X-Forwarded-For must never be
        // honored in that case, or a client could spoof it to dodge rate limiting entirely by
        // sending a different fake IP on every request.
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("198.51.100.9");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(request, times(6)).getRemoteAddr();
    }

    @Test
    void doFilterInternal_HonorsXForwardedForOnlyWhenRemoteAddrIsATrustedProxy() throws Exception {
        RateLimitingFilter trustingFilter = new RateLimitingFilter("10.0.0.0/8");
        when(request.getRequestURI()).thenReturn("/auth/login");
        // The immediate TCP peer is inside the configured trusted-proxy range...
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        // ...so the real client identity behind it (the first XFF hop) is what gets
        // rate-limited, not the proxy's own address.
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");

        for (int i = 0; i < 5; i++) {
            trustingFilter.doFilterInternal(request, response, filterChain);
        }
        trustingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
    }

    @Test
    void doFilterInternal_DoesNotTrustXForwardedForWhenRemoteAddrIsOutsideTheTrustedRange() throws Exception {
        RateLimitingFilter trustingFilter = new RateLimitingFilter("10.0.0.0/8");
        when(request.getRequestURI()).thenReturn("/auth/login");
        // Peer is NOT inside the trusted range — an attacker connecting directly and forging
        // the header must not be able to launder their identity through it.
        when(request.getRemoteAddr()).thenReturn("198.51.100.9");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");

        for (int i = 0; i < 5; i++) {
            trustingFilter.doFilterInternal(request, response, filterChain);
        }
        trustingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
    }

    /**
     * DEM-80: /auth/reset-password redeems a token, so unlimited attempts make that token
     * brute-forceable. forgot-password (which *issues* the token) was already limited, so leaving
     * the redemption side open guarded the cheaper half of the attack only.
     */
    @Test
    void doFilterInternal_ResetPassword_IsRateLimitedLikeTheOtherCredentialEndpoints() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/reset-password");
        when(request.getRemoteAddr()).thenReturn("10.0.0.9");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, times(5)).doFilter(request, response);
    }

    /**
     * The Mentor endpoints call a paid LLM API on every request; unlike the progression group
     * they had no rate limit at all before, so an authenticated user could generate unlimited
     * chat completions with only the manual app.mentor.enabled kill switch as a brake.
     */
    @Test
    void doFilterInternal_MentorChat_IsRateLimitedAt20PerMinute() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/mentor/chat");
        when(request.getRemoteAddr()).thenReturn("10.0.0.30");

        for (int i = 0; i < 20; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, times(20)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_MentorSuggestions_IsRateLimitedAt20PerMinute() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/mentor/suggestions");
        when(request.getRemoteAddr()).thenReturn("10.0.0.31");

        for (int i = 0; i < 20; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, times(20)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_MentorChatAndSuggestions_TrackedIndependentlyPerPathForSameIp() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.32");

        when(request.getRequestURI()).thenReturn("/api/mentor/chat");
        for (int i = 0; i < 20; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        when(request.getRequestURI()).thenReturn("/api/mentor/suggestions");
        filter.doFilterInternal(request, response, filterChain);

        verify(response, never()).setStatus(429);
    }

    /**
     * DEM-78: requestLog never dropped a key once created, so a client that made one request
     * and never came back left a permanent entry — unbounded growth over the app's lifetime.
     */
    @Test
    void cleanupStaleKeys_RemovesKeysWhoseTimestampsHaveAllExpired() {
        filter.trackForTesting("10.0.0.20:/auth/login", Instant.now().minus(Duration.ofHours(1)));
        assertThat(filter.trackedKeyCount()).isEqualTo(1);

        filter.cleanupStaleKeys();

        assertThat(filter.trackedKeyCount()).isZero();
    }

    @Test
    void cleanupStaleKeys_KeepsKeysWithAtLeastOneTimestampStillInsideTheWindow() {
        filter.trackForTesting("10.0.0.21:/auth/login", Instant.now().minus(Duration.ofHours(1)));
        filter.trackForTesting("10.0.0.21:/auth/login", Instant.now());

        filter.cleanupStaleKeys();

        assertThat(filter.trackedKeyCount()).isEqualTo(1);
    }

    @Test
    void cleanupStaleKeys_LeavesUnrelatedActiveKeysAlone() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.22");
        filter.doFilterInternal(request, response, filterChain);
        assertThat(filter.trackedKeyCount()).isEqualTo(1);

        filter.trackForTesting("10.0.0.23:/auth/login", Instant.now().minus(Duration.ofHours(1)));
        assertThat(filter.trackedKeyCount()).isEqualTo(2);

        filter.cleanupStaleKeys();

        assertThat(filter.trackedKeyCount()).isEqualTo(1);
    }

    /**
     * Every quote-shaped endpoint spends the Brapi quota (a paid, per-call provider) on a cache
     * miss, and a miss is exactly what a distinct ticker produces — so the abuse shape here is
     * "iterate symbols", not "hammer one symbol". The bucket therefore has to be shared across
     * the whole rule rather than keyed per URI, or each new ticker would simply open its own.
     */
    @Test
    void doFilterInternal_MarketDataEndpoints_ShareOneBucketAcrossDifferentTickers() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.40");

        for (int i = 0; i < 60; i++) {
            when(request.getRequestURI()).thenReturn("/api/investments/quote/TICKER" + i);
            filter.doFilterInternal(request, response, filterChain);
        }
        when(request.getRequestURI()).thenReturn("/api/investments/quote/PETR4");
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, times(60)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_MarketDataEndpoints_ShareOneBucketAcrossSearchQuoteAndAssetDetails() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.41");

        for (int i = 0; i < 20; i++) {
            when(request.getRequestURI()).thenReturn("/api/investments/search");
            filter.doFilterInternal(request, response, filterChain);
            when(request.getRequestURI()).thenReturn("/api/investments/quote/PETR" + i);
            filter.doFilterInternal(request, response, filterChain);
            when(request.getRequestURI()).thenReturn("/api/investments/asset-details/VALE" + i);
            filter.doFilterInternal(request, response, filterChain);
        }
        when(request.getRequestURI()).thenReturn("/api/investments/search");
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, times(60)).doFilter(request, response);
    }

    /**
     * Same per-URI bucket flaw as the market-data rule above: the lesson id is part of the path,
     * so before the bucket was shared, "complete a different lesson" reset the allowance every
     * time and this rule limited nothing an abuser would actually do.
     */
    @Test
    void doFilterInternal_LessonCompletion_SharesOneBucketAcrossDifferentLessonIds() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.42");

        for (int i = 0; i < 60; i++) {
            when(request.getRequestURI()).thenReturn("/api/v1/learning/lessons/lesson-" + i + "/complete");
            filter.doFilterInternal(request, response, filterChain);
        }
        when(request.getRequestURI()).thenReturn("/api/v1/learning/lessons/lesson-999/complete");
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
    }

    /**
     * /auth/google is a login endpoint that additionally makes an outbound call to Google to
     * verify the ID token, with no session required to reach it — it belongs in the same
     * credential group as /auth/login rather than being the one unlimited way in.
     */
    @Test
    void doFilterInternal_GoogleLogin_IsRateLimitedLikeTheOtherCredentialEndpoints() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/google");
        when(request.getRemoteAddr()).thenReturn("10.0.0.43");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, times(5)).doFilter(request, response);
    }

    /**
     * Looser than the credential group on purpose: refresh is an automated, bursty client
     * action (every 401 in flight can trigger one), not a human typing a password, so a
     * 5/minute cap would break legitimate use. It is here as an abuse backstop only.
     */
    @Test
    void doFilterInternal_Refresh_IsRateLimitedAt30PerMinute() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/refresh");
        when(request.getRemoteAddr()).thenReturn("10.0.0.44");

        for (int i = 0; i < 30; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, times(30)).doFilter(request, response);
    }

    /**
     * Account deletion is irreversible and has no grace period, so it should never be reachable
     * at any volume — this is the backstop against a scripted run through stolen tokens.
     */
    @Test
    void doFilterInternal_AccountDeletion_IsRateLimited() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/settings/account");
        when(request.getRemoteAddr()).thenReturn("10.0.0.45");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
    }
}
