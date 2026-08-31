package com.jf.PetApp.infrastructure.controller.gamification;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jf.PetApp.application.gamification.dto.AchievementEvaluationResult;
import com.jf.PetApp.application.gamification.usecase.EvaluateAchievementsUseCase;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = AchievementController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class AchievementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluateAchievementsUseCase evaluateAchievementsUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "investor@test.com")
    void getAchievements_ReturnsUnlockedAchievementsAndXpTotal() throws Exception {
        Instant unlockedAt = Instant.parse("2026-01-01T00:00:00Z");
        when(evaluateAchievementsUseCase.execute(eq("investor@test.com")))
                .thenReturn(new AchievementEvaluationResult(
                        Map.of("FIRST_INVESTMENT", unlockedAt), Set.of("FIRST_INVESTMENT"), 50));

        mockMvc.perform(get("/api/v1/achievements").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlockedAt.FIRST_INVESTMENT").exists())
                .andExpect(jsonPath("$.newlyUnlockedCodes[0]").value("FIRST_INVESTMENT"))
                .andExpect(jsonPath("$.achievementXpTotal").value(50));
    }
}
