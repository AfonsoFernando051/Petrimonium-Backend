package com.jf.PetApp.infrastructure.controller.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.core.domain.enums.AppContextEnum;

/**
 * Exercises the real {@code /auth/register} and {@code /auth/login} endpoints over actual HTTP,
 * through the real security filter chain and a real (H2) database — the one gap neither
 * {@link AuthControllerTest} (mocked use cases, so nothing here proves the password encoder,
 * refresh-token issuance or the DB unique constraint actually work) nor
 * {@link com.jf.PetApp.infrastructure.config.SecurityConfigTest} (mints tokens directly via
 * {@link TokenProvider}, never once calling {@code /auth/login} to get one) closes: nothing in
 * this codebase previously proved that a token minted by a real login request actually authorizes
 * the app it claims to have been issued for.
 *
 * <p>This lives in {@code controller/auth}, not under any one product's test tree, because
 * {@code jf_users} is the single account every Petrimonium app (Wallet/Academy/Health)
 * authenticates against — see {@code docs/INTEGRATION.md} §5.1 and
 * {@code docs/ARCHITECTURE/fatias/01-auth-e-app-context.md}. The
 * {@link #theSameAccountLogsIntoAllThreeAppsWithASeparateTokenPerContext()} scenario below is
 * exactly that "one account, three apps" contract, verified against the real endpoints instead of
 * asserted from reading the code.
 *
 * <p><b>Rate-limit budget, read this before adding a test:</b> {@code /auth/register} and
 * {@code /auth/login} are each capped at 5 requests/60s per client IP by
 * {@link com.jf.PetApp.infrastructure.security.ratelimit.RateLimitingFilter} — a real production
 * control, not a test artifact — and that limiter is a singleton bean, so its counters are shared
 * by every {@code @SpringBootTest} class Spring's context cache reuses within the same JVM.
 * {@code @DirtiesContext(BEFORE_CLASS)} gives this class its own fresh bucket regardless of what
 * {@link com.jf.PetApp.infrastructure.config.SecurityConfigTest} already spent, but the three
 * tests below still add up to exactly 4 {@code /auth/register} calls and exactly 5
 * {@code /auth/login} calls — the whole budget for that path. Do not add a fourth test that calls
 * {@code login(...)} without first trimming an existing call, or it will start failing with 429
 * instead of the status it expects. {@code /auth/refresh} and {@code /auth/logout} carry no such
 * limit (see the rule's path list) and can be called freely.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AuthEndToEndFlowTest {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {};

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenProvider tokenProvider;

    // ------------------------------------------------------------------ HTTP helpers

    private HttpEntity<Map<String, String>> jsonBody(Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<Map<String, Object>> register(String email, String password) {
        Map<String, String> body = Map.of("username", "e2e-tester", "email", email, "password", password);
        return restTemplate.exchange("/auth/register", HttpMethod.POST, jsonBody(body), JSON_OBJECT);
    }

    private ResponseEntity<Map<String, Object>> login(String email, String password, String appContext) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("appContext", appContext);
        return restTemplate.exchange("/auth/login", HttpMethod.POST, jsonBody(body), JSON_OBJECT);
    }

    private ResponseEntity<Map<String, Object>> refresh(String refreshToken) {
        return restTemplate.exchange("/auth/refresh", HttpMethod.POST,
                jsonBody(Map.of("refreshToken", refreshToken)), JSON_OBJECT);
    }

    private ResponseEntity<Void> logout(String refreshToken) {
        return restTemplate.exchange("/auth/logout", HttpMethod.POST,
                jsonBody(Map.of("refreshToken", refreshToken)), Void.class);
    }

    /** `/api/investments/**` is Wallet-only (see SecurityConfigTest) — a convenient, already
     * proven-safe stand-in (no onboarding needed, mock quote data with no token configured) for
     * "does this access token actually reach a gated controller." */
    private ResponseEntity<String> callWalletGatedEndpoint(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange("/api/investments/quote/PETR4", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
    }

    private String uniqueEmail(String label) {
        return "e2e-" + label + "-" + UUID.randomUUID() + "@petrimonium.test";
    }

    // ---------------------------------------------------------------------------- tests

    @Test
    void theSameAccountLogsIntoAllThreeAppsWithASeparateTokenPerContext() {
        String email = uniqueEmail("shared");
        assertEquals(HttpStatus.CREATED, register(email, "Str0ngPass1").getStatusCode());

        ResponseEntity<Map<String, Object>> walletLogin = login(email, "Str0ngPass1", "wallet");
        ResponseEntity<Map<String, Object>> academyLogin = login(email, "Str0ngPass1", "academy");
        ResponseEntity<Map<String, Object>> healthLogin = login(email, "Str0ngPass1", "health");

        assertEquals(HttpStatus.OK, walletLogin.getStatusCode());
        assertEquals(HttpStatus.OK, academyLogin.getStatusCode());
        assertEquals(HttpStatus.OK, healthLogin.getStatusCode());

        String walletToken = (String) walletLogin.getBody().get("accessToken");
        String academyToken = (String) academyLogin.getBody().get("accessToken");
        String healthToken = (String) healthLogin.getBody().get("accessToken");

        // Same account — proven by the JWT subject, not by assuming three logins with the same
        // email must mean one account. Register only ever ran once above.
        assertEquals(email, tokenProvider.extractSubject(walletToken));
        assertEquals(email, tokenProvider.extractSubject(academyToken));
        assertEquals(email, tokenProvider.extractSubject(healthToken));

        // But three independent sessions, each carrying the context it actually asked for.
        assertEquals(AppContextEnum.WALLET, tokenProvider.extractAppContext(walletToken).orElseThrow());
        assertEquals(AppContextEnum.ACADEMY, tokenProvider.extractAppContext(academyToken).orElseThrow());
        assertEquals(AppContextEnum.HEALTH, tokenProvider.extractAppContext(healthToken).orElseThrow());
        assertNotEquals(walletToken, academyToken);
        assertNotEquals(academyToken, healthToken);
    }

    @Test
    void duplicateRegistrationIsRejectedAndWrongPasswordDoesNotLeakWhichFieldFailed() {
        String email = uniqueEmail("dup");
        assertEquals(HttpStatus.CREATED, register(email, "Str0ngPass1").getStatusCode());

        ResponseEntity<Map<String, Object>> duplicate = register(email, "AnotherStr0ng1");
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        assertEquals("USER_ALREADY_EXISTS", duplicate.getBody().get("code"));

        // Same account, real BCrypt-backed password check this time (not a mocked
        // PasswordEncoderPort like LoginUseCaseImplTest uses) — the wrong password must fail the
        // same way an unknown email would (already unit-tested), never revealing which was wrong.
        ResponseEntity<Map<String, Object>> wrongPassword = login(email, "totally-wrong-pw", "wallet");
        assertEquals(HttpStatus.UNAUTHORIZED, wrongPassword.getStatusCode());
        assertEquals("INVALID_CREDENTIALS", wrongPassword.getBody().get("code"));
    }

    @Test
    void aRealLoginTokenAuthorizesItsAppAndSurvivesRefreshUntilLogoutRevokesIt() {
        String email = uniqueEmail("lifecycle");
        assertEquals(HttpStatus.CREATED, register(email, "Str0ngPass1").getStatusCode());

        ResponseEntity<Map<String, Object>> loginResponse = login(email, "Str0ngPass1", "wallet");
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        String accessToken = (String) loginResponse.getBody().get("accessToken");
        String refreshToken = (String) loginResponse.getBody().get("refreshToken");

        assertEquals(HttpStatus.OK, callWalletGatedEndpoint(accessToken).getStatusCode());

        ResponseEntity<Map<String, Object>> refreshed = refresh(refreshToken);
        assertEquals(HttpStatus.OK, refreshed.getStatusCode());
        String newAccessToken = (String) refreshed.getBody().get("accessToken");
        String newRefreshToken = (String) refreshed.getBody().get("refreshToken");
        assertNotEquals(refreshToken, newRefreshToken, "refresh must rotate the token, never reissue the same one");
        assertEquals(AppContextEnum.WALLET, tokenProvider.extractAppContext(newAccessToken).orElseThrow(),
                "a refresh must inherit the context stamped at login, since the client never resends it");
        assertEquals(HttpStatus.OK, callWalletGatedEndpoint(newAccessToken).getStatusCode());

        assertEquals(HttpStatus.OK, logout(newRefreshToken).getStatusCode());

        // Both the just-revoked (by logout) and the already-rotated-away-from (by refresh) tokens
        // must be dead now — this also doubles as a live check of the "revoked reuse kills every
        // session" rule (fatia 01 §4.3), since presenting either is a case of reusing a revoked token.
        assertEquals(HttpStatus.UNAUTHORIZED, refresh(newRefreshToken).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, refresh(refreshToken).getStatusCode());
    }
}
