package com.jf.PetApp.infrastructure.controller.onboarding;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jf.PetApp.application.onboarding.usecase.SubmitAssessmentUseCase;
import com.jf.PetApp.core.domain.assessment.AcademyOnboardingAnswers;
import com.jf.PetApp.core.domain.assessment.ExperienceLevel;
import com.jf.PetApp.core.domain.assessment.FinancialGoal;
import com.jf.PetApp.core.domain.assessment.InvestmentHorizon;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

import java.util.Optional;

@WebMvcTest(controllers = OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SubmitAssessmentUseCase submitAssessmentUseCase;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldSubmitAssessmentAndReturnProfile() throws Exception {
        AcademyOnboardingAnswers expected = new AcademyOnboardingAnswers(
                FinancialGoal.INVEST_WITH_CONFIDENCE, InvestmentHorizon.MORE_THAN_FIVE_YEARS, ExperienceLevel.PRACTITIONER);
        when(submitAssessmentUseCase.execute(eq("test@example.com"), eq(expected)))
            .thenReturn(InvestorProfile.TACTICIAN);

        OnboardingController.SubmitAssessmentRequestDTO request = new OnboardingController.SubmitAssessmentRequestDTO(
                FinancialGoal.INVEST_WITH_CONFIDENCE, InvestmentHorizon.MORE_THAN_FIVE_YEARS, ExperienceLevel.PRACTITIONER);

        mockMvc.perform(post("/api/onboarding/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("TACTICIAN"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturnCurrentOnboardingStatusAndProfile() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        user.setHasAnsweredOnboarding(true);
        user.setInvestorProfile(InvestorProfile.ADVENTURER);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/onboarding/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAnswered").value(true))
                .andExpect(jsonPath("$.profile").value("ADVENTURER"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturnNullProfileWhenOnboardingNotAnsweredYet() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        user.setHasAnsweredOnboarding(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/onboarding/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAnswered").value(false))
                .andExpect(jsonPath("$.profile").value(nullValue()));
    }
}
