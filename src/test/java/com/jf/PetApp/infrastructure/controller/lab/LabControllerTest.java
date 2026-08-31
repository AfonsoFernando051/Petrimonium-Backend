package com.jf.PetApp.infrastructure.controller.lab;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jf.PetApp.application.lab.dto.SimulatorCompletionResult;
import com.jf.PetApp.application.lab.dto.SimulatorProgressResult;
import com.jf.PetApp.application.lab.usecase.CompleteSimulatorUseCase;
import com.jf.PetApp.application.lab.usecase.GetSimulatorProgressUseCase;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = LabController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class LabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompleteSimulatorUseCase completeSimulatorUseCase;

    @MockitoBean
    private GetSimulatorProgressUseCase getSimulatorProgressUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "learner@test.com")
    void completeSimulator_FirstCompletion_ReturnsAwardedXp() throws Exception {
        when(completeSimulatorUseCase.execute(eq("learner@test.com"), eq("inflation")))
                .thenReturn(new SimulatorCompletionResult("inflation", false, 50, 50, 2, 0, 100));

        mockMvc.perform(post("/api/v1/lab/simulators/inflation/complete")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulatorId").value("inflation"))
                .andExpect(jsonPath("$.alreadyCompleted").value(false))
                .andExpect(jsonPath("$.xpAwarded").value(50))
                .andExpect(jsonPath("$.totalXp").value(50));
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void completeSimulator_UnknownId_PropagatesAsBadRequest() throws Exception {
        when(completeSimulatorUseCase.execute(eq("learner@test.com"), eq("not_real")))
                .thenThrow(new IllegalArgumentException("Unknown simulator id: not_real"));

        mockMvc.perform(post("/api/v1/lab/simulators/not_real/complete")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void getProgress_ReturnsCompletedSimulatorsAndTotals() throws Exception {
        when(getSimulatorProgressUseCase.execute(eq("learner@test.com")))
                .thenReturn(new SimulatorProgressResult(Set.of("compound_interest"), 50, 2, 0, 100));

        mockMvc.perform(get("/api/v1/lab/simulators/progress")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSimulatorIds[0]").value("compound_interest"))
                .andExpect(jsonPath("$.totalXp").value(50));
    }
}
