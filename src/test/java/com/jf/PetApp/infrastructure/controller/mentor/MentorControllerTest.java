package com.jf.PetApp.infrastructure.controller.mentor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.jf.PetApp.application.mentor.dto.ConversationDetailDTO;
import com.jf.PetApp.application.mentor.dto.ConversationSummaryDTO;
import com.jf.PetApp.application.mentor.dto.MentorChatResponse;
import com.jf.PetApp.application.mentor.usecase.DeleteConversationUseCase;
import com.jf.PetApp.application.mentor.usecase.GetConversationUseCase;
import com.jf.PetApp.application.mentor.usecase.GetMentorReplyUseCase;
import com.jf.PetApp.application.mentor.usecase.ListConversationsUseCase;
import com.jf.PetApp.application.mentor.usecase.MentorPromptSuggestionsService;
import com.jf.PetApp.application.mentor.usecase.RenameConversationUseCase;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

import java.time.Instant;
import java.util.List;

@WebMvcTest(controllers = MentorController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class MentorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMentorReplyUseCase getMentorReplyUseCase;
    @MockitoBean
    private ListConversationsUseCase listConversationsUseCase;
    @MockitoBean
    private GetConversationUseCase getConversationUseCase;
    @MockitoBean
    private RenameConversationUseCase renameConversationUseCase;
    @MockitoBean
    private DeleteConversationUseCase deleteConversationUseCase;
    @MockitoBean
    private MentorPromptSuggestionsService mentorPromptSuggestionsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "investor@test.com")
    void chat_WithValidMessage_ReturnsMentorReply() throws Exception {
        when(getMentorReplyUseCase.execute(eq("investor@test.com"), any(), any()))
                .thenReturn(new MentorChatResponse(
                        "Looks like a solid diversified portfolio!", 1L, "How is my portfolio doing?", List.of("portfolio_summary")));

        mockMvc.perform(post("/api/mentor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"How is my portfolio doing?"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Looks like a solid diversified portfolio!"))
                .andExpect(jsonPath("$.conversationId").value(1))
                .andExpect(jsonPath("$.title").value("How is my portfolio doing?"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void suggestions_ReturnsTheBackendSample() throws Exception {
        when(mentorPromptSuggestionsService.getRandomSuggestions("pt", 5))
                .thenReturn(List.of("Como começar a investir do zero?", "O que é um ETF?"));

        mockMvc.perform(get("/api/mentor/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0]").value("Como começar a investir do zero?"))
                .andExpect(jsonPath("$.suggestions[1]").value("O que é um ETF?"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void chat_WithBlankMessage_Returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/mentor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void chat_WithMessageOverSizeLimit_Returns400ValidationError() throws Exception {
        String tooLong = "a".repeat(2001);

        mockMvc.perform(post("/api/mentor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void chat_WithContextFieldsOverSizeLimit_Returns400ValidationError() throws Exception {
        String tooLongGoal = "a".repeat(201);

        mockMvc.perform(post("/api/mentor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\",\"context\":{\"petGoal\":\"" + tooLongGoal + "\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void listConversations_ReturnsTheUsersConversations() throws Exception {
        when(listConversationsUseCase.execute(eq("investor@test.com"), any())).thenReturn(List.of(
                new ConversationSummaryDTO(1L, "Dividends 101", Instant.now(), "Dividends are periodic payments...")));

        mockMvc.perform(get("/api/mentor/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Dividends 101"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void getConversation_ReturnsItsMessages() throws Exception {
        when(getConversationUseCase.execute(eq("investor@test.com"), eq(1L), any()))
                .thenReturn(new ConversationDetailDTO(1L, "Dividends 101", List.of()));

        mockMvc.perform(get("/api/mentor/conversations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Dividends 101"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void renameConversation_WithValidTitle_Returns204() throws Exception {
        mockMvc.perform(patch("/api/mentor/conversations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"New title"}"""))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void renameConversation_WithBlankTitle_Returns400ValidationError() throws Exception {
        mockMvc.perform(patch("/api/mentor/conversations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void deleteConversation_Returns204() throws Exception {
        mockMvc.perform(delete("/api/mentor/conversations/1"))
                .andExpect(status().isNoContent());
    }
}
