package com.jf.PetApp.infrastructure.config;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.domain.enums.RoleEnum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @Test
    void protectedEndpoint_WithoutAnyToken_IsRejected() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/investments/quote/PETR4", String.class);

        assertTrue(response.getStatusCode().is4xxClientError());
        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void protectedEndpoint_WithGarbageToken_IsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this-is-not-a-real-jwt");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/investments/quote/PETR4", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertTrue(response.getStatusCode().is4xxClientError());
        assertNotEquals(HttpStatus.OK, response.getStatusCode());
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
    void sharedEndpoint_WithValidTokenButNoAppContext_IsStillReachable() {
        // Not every endpoint is app_context-scoped — gamification/pet/mentor/auth stay reachable
        // by any authenticated session regardless of which app (or no app) issued it.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("investor-shared@test.com", RoleEnum.USER));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/pets/status", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
