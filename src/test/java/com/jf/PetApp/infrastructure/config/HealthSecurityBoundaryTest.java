package com.jf.PetApp.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.health.HealthService;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.domain.enums.RoleEnum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

/**
 * The app_context gate around Health, through the real filter chain: this is the user's actual
 * cash flow, so a Wallet or Academy session must be refused even though it is a perfectly valid
 * Petrimonium session, and a Health session must not gain a foothold in the other two products.
 *
 * <p>{@link HealthService} is mocked because this test is about the gate, not the rules — and
 * because the suite deliberately runs with Flyway disabled (see {@code src/test/resources/
 * application.properties}), so the Health tables only exist in {@code HealthServiceIntegrationTest},
 * which builds them from the real migrations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class HealthSecurityBoundaryTest {

    private static final String HEALTH_ACCOUNTS = "/api/v1/health/accounts";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private HealthService healthService;

    private HttpEntity<Void> session(String email, AppContextEnum appContext) {
        User user = User.create("healthboundary", email, "irrelevant-hash", RoleEnum.USER);
        userRepository.save(user);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.generateToken(user, appContext));
        return new HttpEntity<>(headers);
    }

    private HttpStatus statusOf(String path, HttpEntity<Void> session) {
        ResponseEntity<String> response = restTemplate.exchange(path, HttpMethod.GET, session, String.class);
        return HttpStatus.valueOf(response.getStatusCode().value());
    }

    @Test
    void healthEndpoint_WithoutAToken_IsRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity(HEALTH_ACCOUNTS, String.class);

        assertTrue(response.getStatusCode().is4xxClientError());
        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void healthEndpoint_WithGarbageToken_IsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this-is-not-a-real-jwt");

        ResponseEntity<String> response = restTemplate.exchange(HEALTH_ACCOUNTS, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertTrue(response.getStatusCode().is4xxClientError());
        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void healthEndpoint_WithValidTokenButNoAppContext_IsForbidden() {
        assertEquals(HttpStatus.FORBIDDEN,
                statusOf(HEALTH_ACCOUNTS, session("health-no-context@test.com", null)));
    }

    @Test
    void healthEndpoint_WithWalletAppContext_IsForbidden() {
        assertEquals(HttpStatus.FORBIDDEN,
                statusOf(HEALTH_ACCOUNTS, session("health-from-wallet@test.com", AppContextEnum.WALLET)));
    }

    @Test
    void healthEndpoint_WithAcademyAppContext_IsForbidden() {
        // Academy's money is simulated. It must never read a real balance.
        assertEquals(HttpStatus.FORBIDDEN,
                statusOf(HEALTH_ACCOUNTS, session("health-from-academy@test.com", AppContextEnum.ACADEMY)));
    }

    @Test
    void healthEndpoint_WithHealthAppContext_ReachesTheController() {
        org.mockito.Mockito.when(healthService.listAccounts("health-app@test.com")).thenReturn(List.of());

        assertEquals(HttpStatus.OK,
                statusOf(HEALTH_ACCOUNTS, session("health-app@test.com", AppContextEnum.HEALTH)));
    }

    @Test
    void healthSession_CannotReachTheRealPortfolio() {
        assertEquals(HttpStatus.FORBIDDEN,
                statusOf("/api/investments/quote/PETR4", session("health-wants-wallet@test.com",
                        AppContextEnum.HEALTH)));
    }

    @Test
    void healthSession_CannotReachTheSimulatedPortfolio() {
        assertEquals(HttpStatus.FORBIDDEN,
                statusOf("/api/v1/simulated-portfolios/me", session("health-wants-simulated@test.com",
                        AppContextEnum.HEALTH)));
    }

    @Test
    void healthSession_CannotReachAcademyContentOrMissions() {
        HttpEntity<Void> session = session("health-wants-academy@test.com", AppContextEnum.HEALTH);

        assertEquals(HttpStatus.FORBIDDEN, statusOf("/api/v1/missions", session));
        assertEquals(HttpStatus.FORBIDDEN, statusOf("/api/v1/achievements", session));
    }

    /**
     * Reversed by DEM-106, deliberately. This used to assert FORBIDDEN because the Mentor could
     * only build a real- or simulated-portfolio prompt, so serving a Health session meant serving
     * it the wrong context. Now that {@code MentorSystemPromptBuilder.buildForHealth} exists, the
     * Mentor is reachable from Health.
     *
     * <p>The boundary did not go away, it moved: it is no longer "Health can't have a Mentor" but
     * "the Mentor a Health session gets is built from cash flow only". That invariant is enforced
     * in {@code GetMentorReplyUseCaseImpl} and pinned by
     * {@code GetMentorReplyUseCaseImplTest#execute_ForHealthContext_NeverCallsAnyRealOrSimulatedPortfolioUseCase},
     * which fails if a Health turn so much as touches a portfolio use case. Every other Health
     * boundary in this class is unchanged.
     */
    @Test
    void healthSession_ReachesTheMentor_WhichAnswersItFromCashFlowOnly() {
        assertEquals(HttpStatus.OK,
                statusOf("/api/mentor/suggestions", session("health-wants-mentor@test.com",
                        AppContextEnum.HEALTH)));
    }

    @Test
    void healthSession_StillReachesTheSharedPetItReuses() {
        // Health does not create a second identity or a second pet: the companion the user already
        // has in Wallet and Academy stays reachable from a Health session.
        assertEquals(HttpStatus.OK,
                statusOf("/api/pets/status", session("health-pet@test.com", AppContextEnum.HEALTH)));
    }
}
