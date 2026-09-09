package com.jf.PetApp.infrastructure.controller.pet;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.jf.PetApp.application.pet.usecase.ConfigurePetUseCase;
import com.jf.PetApp.application.pet.usecase.GetMyPetUseCase;
import com.jf.PetApp.application.pet.usecase.GetPetStatusUseCase;
import com.jf.PetApp.application.pet.usecase.LinkPetToAppUseCase;
import com.jf.PetApp.application.pet.usecase.ListMyPetsUseCase;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = PetController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigurePetUseCase configurePetUseCase;

    @MockitoBean
    private GetPetStatusUseCase getPetStatusUseCase;

    @MockitoBean
    private GetMyPetUseCase getMyPetUseCase;

    @MockitoBean
    private LinkPetToAppUseCase linkPetToAppUseCase;

    @MockitoBean
    private ListMyPetsUseCase listMyPetsUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    private Pet petWith(PetSpecieEnum specie, String name, int health) {
        Pet pet = new Pet();
        pet.setSpecie(specie);
        pet.setName(name);
        pet.setHealth(health);
        return pet;
    }

    @Test
    @WithMockUser(username = "investor@test.com", authorities = "APP_CONTEXT_WALLET")
    void configurePet_WithValidSpecie_InvokesUseCaseAndReturns200() throws Exception {
        mockMvc.perform(post("/api/pets/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specie":"dog"}"""))
                .andExpect(status().isOk());

        verify(configurePetUseCase).execute("investor@test.com", AppContextEnum.WALLET, PetSpecieEnum.DOG, null);
    }

    @Test
    @WithMockUser(username = "investor@test.com", authorities = "APP_CONTEXT_WALLET")
    void configurePet_WithNameProvided_PassesItToTheUseCase() throws Exception {
        mockMvc.perform(post("/api/pets/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specie":"dog","name":"Rex"}"""))
                .andExpect(status().isOk());

        verify(configurePetUseCase).execute("investor@test.com", AppContextEnum.WALLET, PetSpecieEnum.DOG, "Rex");
    }

    @Test
    @WithMockUser(username = "investor@test.com", authorities = "APP_CONTEXT_WALLET")
    void configurePet_WithUnknownSpecie_Returns400() throws Exception {
        mockMvc.perform(post("/api/pets/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specie":"dragon"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "investor@test.com", authorities = "APP_CONTEXT_ACADEMY")
    void configurePet_WithSpecieNotMatchingAppContext_Returns400() throws Exception {
        // Academy's fixed mascot is WOLF (AppContextEnum#defaultPetSpecie), so a Wallet-shaped
        // request landing on an Academy session must be rejected rather than silently accepted.
        mockMvc.perform(post("/api/pets/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specie":"dog"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void configurePet_WithNoAppContext_Returns400() throws Exception {
        mockMvc.perform(post("/api/pets/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specie":"dog"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "investor@test.com", authorities = "APP_CONTEXT_WALLET")
    void getStatus_WhenUserHasPet_ReturnsTrue() throws Exception {
        when(getPetStatusUseCase.execute(eq("investor@test.com"), eq(AppContextEnum.WALLET))).thenReturn(true);

        mockMvc.perform(get("/api/pets/status").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPet").value(true));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void getStatus_WithNoAppContext_ReturnsFalseWithoutCallingTheUseCase() throws Exception {
        mockMvc.perform(get("/api/pets/status").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPet").value(false));
    }

    @Test
    @WithMockUser(username = "investor@test.com", authorities = "APP_CONTEXT_HEALTH")
    void getMyPet_WhenPetExists_ReturnsPetDetails() throws Exception {
        when(getMyPetUseCase.execute(eq("investor@test.com"), eq(AppContextEnum.HEALTH)))
                .thenReturn(Optional.of(petWith(PetSpecieEnum.FOX, "Rusty", 80)));

        mockMvc.perform(get("/api/pets/my-pet").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specie").value("FOX"))
                .andExpect(jsonPath("$.name").value("Rusty"))
                .andExpect(jsonPath("$.health").value(80));
    }

    @Test
    @WithMockUser(username = "investor@test.com", authorities = "APP_CONTEXT_HEALTH")
    void getMyPet_WhenNoPetConfigured_Returns404() throws Exception {
        when(getMyPetUseCase.execute(eq("investor@test.com"), eq(AppContextEnum.HEALTH))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pets/my-pet").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "investor@test.com", authorities = "APP_CONTEXT_HEALTH")
    void linkPetToCurrentApp_InvokesUseCaseWithTheCurrentAppContext() throws Exception {
        mockMvc.perform(post("/api/pets/7/link"))
                .andExpect(status().isOk());

        verify(linkPetToAppUseCase).execute("investor@test.com", 7, AppContextEnum.HEALTH);
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void linkPetToCurrentApp_WithNoAppContext_Returns400() throws Exception {
        mockMvc.perform(post("/api/pets/7/link"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "investor@test.com", authorities = "APP_CONTEXT_WALLET")
    void listMyPets_ReturnsEveryPetTheUserOwns() throws Exception {
        Pet dog = petWith(PetSpecieEnum.DOG, "Rex", 100);
        dog.setId(7);
        when(listMyPetsUseCase.execute("investor@test.com")).thenReturn(java.util.List.of(dog));

        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].specie").value("DOG"))
                .andExpect(jsonPath("$[0].name").value("Rex"));
    }
}
