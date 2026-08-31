package com.jf.PetApp.infrastructure.controller.academy;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.application.academy.dto.AcademyDomainView;
import com.jf.PetApp.application.academy.dto.AcademyLessonStepView;
import com.jf.PetApp.application.academy.dto.AcademyLessonView;
import com.jf.PetApp.application.academy.dto.AcademyModuleView;
import com.jf.PetApp.application.academy.dto.AcademySchoolView;
import com.jf.PetApp.application.academy.usecase.GetAcademyCatalogUseCase;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = AcademyCatalogController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only the web layer
class AcademyCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAcademyCatalogUseCase getAcademyCatalogUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "learner@test.com")
    void getCatalog_DefaultsToPortuguese_AndReturnsTheFullShape() throws Exception {
        AcademyCatalogResult result = new AcademyCatalogResult(
                List.of(new AcademyDomainView("financial_education", "Educação Financeira", "desc", "savings_outlined", 1,
                        List.of("financial_life"))),
                List.of(new AcademySchoolView("financial_life", "financial_education", "Vida Financeira", "desc",
                        "savings_outlined", 1, List.of(), true)),
                List.of(new AcademyModuleView("money_fundamentals", "financial_life", "Fundamentos do Dinheiro", "desc",
                        "payments_outlined", "FOUNDATION", 1, List.of("money_fundamentals_what_is_money"), List.of(), true)),
                List.of(new AcademyLessonView("money_fundamentals_what_is_money", "money_fundamentals", "O que é Dinheiro?",
                        "Explicar as tres funcoes do dinheiro.", "EXPLAIN", 3, 1, 20, List.of("pe", "dy"),
                        List.of(new AcademyLessonStepView("choice_question", null, null, "micro_exercise",
                                List.of("A", "B"), 1, "Prompt?", "Because.", List.of())))));

        when(getAcademyCatalogUseCase.execute(eq("pt"))).thenReturn(result);

        mockMvc.perform(get("/api/v1/academy/catalog").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domains[0].id").value("financial_education"))
                .andExpect(jsonPath("$.schools[0].id").value("financial_life"))
                .andExpect(jsonPath("$.modules[0].id").value("money_fundamentals"))
                .andExpect(jsonPath("$.lessons[0].id").value("money_fundamentals_what_is_money"))
                .andExpect(jsonPath("$.lessons[0].portfolioConcepts[0]").value("pe"))
                .andExpect(jsonPath("$.lessons[0].portfolioConcepts[1]").value("dy"))
                .andExpect(jsonPath("$.lessons[0].steps[0].type").value("choice_question"))
                .andExpect(jsonPath("$.lessons[0].steps[0].correctIndex").value(1));
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void getCatalog_WithExplicitLang_PassesItThrough() throws Exception {
        when(getAcademyCatalogUseCase.execute(eq("en")))
                .thenReturn(new AcademyCatalogResult(List.of(), List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/academy/catalog").param("lang", "en").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void getCatalog_WithUnsupportedLang_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/academy/catalog").param("lang", "fr").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
