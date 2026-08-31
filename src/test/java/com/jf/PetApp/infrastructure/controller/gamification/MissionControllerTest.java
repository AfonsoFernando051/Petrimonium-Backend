package com.jf.PetApp.infrastructure.controller.gamification;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jf.PetApp.application.gamification.dto.MissionEvaluationResult;
import com.jf.PetApp.application.gamification.dto.MissionStatusDTO;
import com.jf.PetApp.application.gamification.usecase.EvaluateMissionsUseCase;
import com.jf.PetApp.core.domain.gamification.MissionPeriod;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = MissionController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class MissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluateMissionsUseCase evaluateMissionsUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "learner@test.com")
    void getMissions_ReturnsMissionStatusesAndXpTotal() throws Exception {
        MissionStatusDTO status = new MissionStatusDTO(
                "daily_complete_lesson", MissionPeriod.DAILY, "2026-08-19", 1, 1, 30, true);
        when(evaluateMissionsUseCase.execute(eq("learner@test.com")))
                .thenReturn(new MissionEvaluationResult(
                        List.of(status), Set.of("daily_complete_lesson"), 30));

        mockMvc.perform(get("/api/v1/missions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missions[0].code").value("daily_complete_lesson"))
                .andExpect(jsonPath("$.missions[0].completed").value(true))
                .andExpect(jsonPath("$.newlyCompletedCodes[0]").value("daily_complete_lesson"))
                .andExpect(jsonPath("$.missionXpTotal").value(30));
    }
}
