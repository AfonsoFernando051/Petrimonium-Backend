package com.jf.PetApp.infrastructure.config;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.domain.enums.RoleEnum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end slice through the real {@link SecurityFilterChain} (not a mocked-out
 * {@code @WebMvcTest}) — verifies the actual authorization rule ({@code /auth/**} open,
 * everything else needs a valid bearer token) as configured in {@link SecurityConfig}, plus
 * that {@link com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter} is really
 * wired into the chain.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SecurityConfigTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    // Minted directly rather than through TokenProvider: the port deliberately exposes no way to
    // issue an already-expired token, and this test needs exactly that — the shape a real client
    // presents once jwt.expiration has elapsed.
    @Value("${jwt.secret}")
    private String jwtSecret;

    private String expiredTokenFor(User user) {
        Date expiredAt = new Date(System.currentTimeMillis() - 60_000);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("app_context", AppContextEnum.WALLET.claimValue())
                .issuedAt(new Date(expiredAt.getTime() - 60_000))
                .expiration(expiredAt)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    // JwtAuthenticationFilter re-looks-up the user by email on every request (see its
    // doFilterInternal) rather than trusting the JWT's claims — so a token is only "valid" in
    // the sense this test needs if the email it carries actually resolves to a persisted user.
    private String validTokenFor(String email, RoleEnum role) {
        return validTokenFor(email, role, null);
    }

    private String validTokenFor(String email, RoleEnum role, AppContextEnum appContext) {
        User user = User.create("securitytest", email, "irrelevant-hash", role);
        userRepository.save(user);
        return tokenProvider.generateToken(user, appContext);
    }

    @Test
    void authLogin_IsReachableWithoutAToken() {
        // A malformed body still reaches AuthController (and fails validation there, 400) —
        // never blocked at the security layer with 401/403 — proving /auth/** is permitAll.
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/login", "{}", String.class);

        assertNotEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void authForgotPassword_IsReachableWithoutAToken() {
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/forgot-password", "{}", String.class);

        assertNotEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // 401, not merely "some 4xx": the distinction is load bearing for the mobile client, whose
    // ApiClient._sendWithAuth refreshes-and-retries on exactly 401 and passes anything else
    // straight to the caller. Spring Security's default entry point when neither httpBasic nor
    // formLogin is enabled is Http403ForbiddenEntryPoint, so without an explicit entry point an
    // expired access token answers 403 and the client never refreshes — the session dies after
    // jwt.expiration instead of living the refresh token's 30 days. Asserting only is4xx (as
    // this test used to) is precisely what let that go unnoticed.
    @Test
    void protectedEndpoint_WithoutAnyToken_IsUnauthorized() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/investments/quote/PETR4", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // The same path an *expired* access token takes: JwtAuthenticationFilter's validate() fails,
    // no Authentication is set, and the request arrives at the authorization rules anonymous.
    @Test
    void protectedEndpoint_WithGarbageToken_IsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this-is-not-a-real-jwt");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/investments/quote/PETR4", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // An expired token specifically — the case the mobile client hits every hour in normal use.
    @Test
    void protectedEndpoint_WithExpiredToken_IsUnauthorized() {
        User user = User.create("expiredtest", "expired-token@test.com", "irrelevant-hash", RoleEnum.USER);
        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredTokenFor(user));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/investments/quote/PETR4", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // Both rejections must carry the same ProblemDetail shape every other error in this API uses
    // (GlobalExceptionHandler's `code`/`timestamp`), because these two are thrown inside the
    // filter chain and never reach @ControllerAdvice — without an explicit handler they answer
    // with an empty body the client cannot parse alongside every other error it handles.
    @Test
    void unauthorizedResponse_CarriesTheStandardProblemDetailShape() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/investments/quote/PETR4", String.class);

        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("\"code\":\"UNAUTHENTICATED\""), response.getBody());
    }

    @Test
    void forbiddenResponse_CarriesTheStandardProblemDetailShape() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("problemdetail-forbidden@test.com", RoleEnum.USER));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/investments/quote/PETR4", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("\"code\":\"ACCESS_DENIED\""), response.getBody());
    }

    @Test
    void protectedEndpoint_WithValidTokenButNoAppContext_IsForbidden() {
        // real_portfolio (docs/BACKEND_MODULE_PLAN.md §5) is Wallet-scoped at this layer — a
        // token with no app_context claim (e.g. minted before this claim existed) can no longer
        // reach it, same as the intended-breaking-change flagged in ECOSYSTEM.md for Academy's
        // current /api/investments callers.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("investor-no-context@test.com", RoleEnum.USER));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/investments/quote/PETR4", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void protectedEndpoint_WithAcademyAppContext_IsForbiddenFromRealPortfolio() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("investor-academy@test.com", RoleEnum.USER, AppContextEnum.ACADEMY));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/investments/quote/PETR4", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void protectedEndpoint_WithWalletAppContext_IsAuthenticatedAndReachesTheController() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("investor-wallet@test.com", RoleEnum.USER, AppContextEnum.WALLET));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/investments/quote/PETR4", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // BrapiInvestmentApiClient falls back to mock data with no token configured, so a
        // valid bearer token should reach the controller and get a normal 200 — not blocked.
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void simulatedPortfolioEndpoint_WithValidTokenButNoAppContext_IsForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("learner-no-context@test.com", RoleEnum.USER));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/simulated-portfolios/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void simulatedPortfolioEndpoint_WithWalletAppContext_IsForbidden() {
        // The inverse of protectedEndpoint_WithAcademyAppContext_IsForbiddenFromRealPortfolio:
        // Wallet must never be able to reach Academy's simulated wallet either.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("investor-wants-simulated@test.com", RoleEnum.USER, AppContextEnum.WALLET));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/simulated-portfolios/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void simulatedPortfolioEndpoint_WithAcademyAppContext_IsAuthenticatedAndReachesTheController() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("learner-academy@test.com", RoleEnum.USER, AppContextEnum.ACADEMY));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/simulated-portfolios/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void sharedEndpoint_WithValidTokenButNoAppContext_IsStillReachable() {
        // Not every endpoint is app_context-scoped — the canonical Pet/gamification summary
        // (/api/pets, /api/v1/gamification) stays reachable by any authenticated session
        // regardless of which app (or no app) issued it: Stage 6 confirmed the pet is meant to be
        // a single cross-app companion, and its XP is already allow-listed to learning/practice
        // events only, so this isn't a leak (see SecurityConfig's comment on this block).
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("investor-shared@test.com", RoleEnum.USER));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/pets/status", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- Stage 6 (Pet/XP/Mentor context separation): Mentor is shared but context-sensitive, so
    // (unlike Pet/gamification above) it now requires a resolvable app_context — an ambiguous
    // session can't safely be served since GetMentorReplyUseCaseImpl needs to know whether to
    // pull the real or the simulated portfolio. Missions/achievements are one-sided like
    // real_portfolio/simulated_portfolio: missions are Academy-only learning content, achievements
    // are Wallet-only wealth-threshold badges. ---

    @Test
    void mentorEndpoint_WithValidTokenButNoAppContext_IsForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("mentor-no-context@test.com", RoleEnum.USER));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/mentor/suggestions", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void mentorEndpoint_WithWalletAppContext_IsAuthenticatedAndReachesTheController() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("mentor-wallet@test.com", RoleEnum.USER, AppContextEnum.WALLET));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/mentor/suggestions", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void mentorEndpoint_WithAcademyAppContext_IsAuthenticatedAndReachesTheController() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("mentor-academy@test.com", RoleEnum.USER, AppContextEnum.ACADEMY));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/mentor/suggestions", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * DEM-106: the Health app's Mentor tab used to 403 here. The gate was opened only once
     * {@code MentorSystemPromptBuilder.buildForHealth} existed — before that, passing this gate
     * would have answered a Health session with the user's real portfolio.
     */
    @Test
    void mentorEndpoint_WithHealthAppContext_IsAuthenticatedAndReachesTheController() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("mentor-health@test.com", RoleEnum.USER, AppContextEnum.HEALTH));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/mentor/suggestions", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void missionsEndpoint_WithWalletAppContext_IsForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("missions-wallet@test.com", RoleEnum.USER, AppContextEnum.WALLET));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/missions", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void missionsEndpoint_WithAcademyAppContext_IsAuthenticatedAndReachesTheController() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("missions-academy@test.com", RoleEnum.USER, AppContextEnum.ACADEMY));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/missions", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void achievementsEndpoint_WithAcademyAppContext_IsForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("achievements-academy@test.com", RoleEnum.USER, AppContextEnum.ACADEMY));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/achievements", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void achievementsEndpoint_WithWalletAppContext_IsAuthenticatedAndReachesTheController() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("achievements-wallet@test.com", RoleEnum.USER, AppContextEnum.WALLET));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/achievements", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
