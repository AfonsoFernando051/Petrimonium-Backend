package com.jf.PetApp.infrastructure.controller.settings;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jf.PetApp.application.settings.usecase.UpdateLanguageUseCase;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = SettingsController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UpdateLanguageUseCase updateLanguageUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "investor@test.com")
    void getLanguage_ReturnsUsersPreferredLanguage() throws Exception {
        User user = new User();
        user.setEmail("investor@test.com");
        user.setPreferredLanguage("en");
        when(userRepository.findByEmail(eq("investor@test.com"))).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/settings/language").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    @WithMockUser(username = "ghost@test.com")
    void getLanguage_WhenUserNotFound_Returns401() throws Exception {
        when(userRepository.findByEmail(eq("ghost@test.com"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/settings/language").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void updateLanguage_ReturnsUpdatedLanguage() throws Exception {
        when(updateLanguageUseCase.execute(eq("investor@test.com"), eq("es"))).thenReturn("es");

        mockMvc.perform(put("/api/settings/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"es"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("es"));
    }
}
