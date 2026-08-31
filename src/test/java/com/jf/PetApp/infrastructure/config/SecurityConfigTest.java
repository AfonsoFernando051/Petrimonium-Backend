package com.jf.PetApp.infrastructure.config;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
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
        User user = User.create("securitytest", email, "irrelevant-hash", role);
        userRepository.save(user);
        return tokenProvider.generateToken(user);
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
    void protectedEndpoint_WithValidToken_IsAuthenticatedAndReachesTheController() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validTokenFor("investor@test.com", RoleEnum.USER));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/investments/quote/PETR4", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // BrapiInvestmentApiClient falls back to mock data with no token configured, so a
        // valid bearer token should reach the controller and get a normal 200 — not blocked.
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
