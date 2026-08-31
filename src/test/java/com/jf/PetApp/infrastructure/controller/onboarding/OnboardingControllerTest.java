package com.jf.PetApp.infrastructure.controller.onboarding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jf.PetApp.application.onboarding.usecase.SubmitAssessmentUseCase;
import com.jf.PetApp.application.translation.service.TranslationCacheService;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.assessment.Option;
import com.jf.PetApp.core.domain.assessment.Question;
import com.jf.PetApp.core.port.QuestionRepository;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private QuestionRepository questionRepository;

    @MockitoBean
    private SubmitAssessmentUseCase submitAssessmentUseCase;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TranslationCacheService translationCacheService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser
    void shouldReturnQuestionsWithoutPoints() throws Exception {
        // Pass-through: this slice only verifies the web layer wiring, not
        // real translation.
        when(translationCacheService.translate(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(questionRepository.findAll()).thenReturn(List.of(
            new Question("q1", "Test Question", List.of(
                new Option("opt1", "Test Option", 5)
            ))
        ));

        mockMvc.perform(get("/api/onboarding/questions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("q1"))
                .andExpect(jsonPath("$[0].text").value("Test Question"))
                .andExpect(jsonPath("$[0].options[0].id").value("opt1"))
                .andExpect(jsonPath("$[0].options[0].text").value("Test Option"))
                .andExpect(jsonPath("$[0].options[0].points").doesNotExist()); // Points should be hidden
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldSubmitAssessmentAndReturnProfile() throws Exception {
        when(submitAssessmentUseCase.execute(eq("test@example.com"), eq(List.of("opt1", "opt2"))))
            .thenReturn(InvestorProfile.TACTICIAN);

        OnboardingController.SubmitAssessmentRequestDTO request =
            new OnboardingController.SubmitAssessmentRequestDTO(List.of("opt1", "opt2"));

        mockMvc.perform(post("/api/onboarding/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("TACTICIAN"));
    }
}
