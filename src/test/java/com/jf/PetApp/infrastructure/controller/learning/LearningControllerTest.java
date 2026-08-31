package com.jf.PetApp.infrastructure.controller.learning;

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

import com.jf.PetApp.application.learning.dto.LearningProgressResult;
import com.jf.PetApp.application.learning.dto.LessonCompletionResult;
import com.jf.PetApp.application.learning.usecase.CompleteLessonUseCase;
import com.jf.PetApp.application.learning.usecase.GetLearningProgressUseCase;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = LearningController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class LearningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompleteLessonUseCase completeLessonUseCase;

    @MockitoBean
    private GetLearningProgressUseCase getLearningProgressUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "learner@test.com")
    void completeLesson_NoRequestBody_TreatsPerfectFirstTryAsFalse() throws Exception {
        when(completeLessonUseCase.execute(eq("learner@test.com"), eq("foundations_what_is_investing"), eq(false)))
                .thenReturn(new LessonCompletionResult("foundations_what_is_investing", false, 20, false, 0, 20, 1, 20, 50));

        mockMvc.perform(post("/api/v1/learning/lessons/foundations_what_is_investing/complete")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyCompleted").value(false))
                .andExpect(jsonPath("$.xpAwarded").value(20))
                .andExpect(jsonPath("$.totalXp").value(20))
                .andExpect(jsonPath("$.level").value(1));
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void completeLesson_PerfectFirstTryInBody_PassesThroughToUseCase() throws Exception {
        when(completeLessonUseCase.execute(eq("learner@test.com"), eq("foundations_what_is_investing"), eq(true)))
                .thenReturn(new LessonCompletionResult("foundations_what_is_investing", false, 20, false, 0, 20, 1, 20, 50));

        mockMvc.perform(post("/api/v1/learning/lessons/foundations_what_is_investing/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"perfectFirstTry\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xpAwarded").value(20));
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void getProgress_ReturnsCompletedLessonsAndTotals() throws Exception {
        when(getLearningProgressUseCase.execute(eq("learner@test.com")))
                .thenReturn(new LearningProgressResult(
                        Set.of("foundations_what_is_investing"), Set.of(), Set.of("foundations_what_is_investing"), 20, 1, 20, 50));

        mockMvc.perform(get("/api/v1/learning/progress")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedLessonIds[0]").value("foundations_what_is_investing"))
                .andExpect(jsonPath("$.perfectLessonIds[0]").value("foundations_what_is_investing"))
                .andExpect(jsonPath("$.totalXp").value(20));
    }
}
