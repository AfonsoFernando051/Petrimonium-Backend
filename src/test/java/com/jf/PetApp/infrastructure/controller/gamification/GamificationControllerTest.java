package com.jf.PetApp.infrastructure.controller.gamification;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jf.PetApp.application.gamification.dto.GamificationSummaryResult;
import com.jf.PetApp.application.gamification.usecase.GetGamificationSummaryUseCase;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = GamificationController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class GamificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetGamificationSummaryUseCase getGamificationSummaryUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "investor@test.com")
    void getSummary_ReturnsXpLevelAndStreakFields() throws Exception {
        when(getGamificationSummaryUseCase.execute(eq("investor@test.com")))
                .thenReturn(new GamificationSummaryResult(120, 3, 20, 50, 5, 12));

        mockMvc.perform(get("/api/v1/gamification/summary").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(120))
                .andExpect(jsonPath("$.level").value(3))
                .andExpect(jsonPath("$.xpIntoLevel").value(20))
                .andExpect(jsonPath("$.xpForNextLevel").value(50))
                .andExpect(jsonPath("$.currentStreak").value(5))
                .andExpect(jsonPath("$.longestStreak").value(12));
    }
}
