package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.application.academy.dto.AcademyDomainView;
import com.jf.PetApp.application.academy.dto.AcademyLessonView;
import com.jf.PetApp.application.academy.dto.AcademyModuleView;
import com.jf.PetApp.application.academy.dto.AcademySchoolView;
import com.jf.PetApp.application.academy.usecase.GetAcademyCatalogUseCase;
import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;
import com.jf.PetApp.application.investment.usecase.GetPortfolioAllocationUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioSummaryUseCase;
import com.jf.PetApp.application.learning.dto.LearningProgressResult;
import com.jf.PetApp.application.learning.usecase.GetLearningProgressUseCase;
import com.jf.PetApp.application.mentor.dto.MentorChatRequest;
import com.jf.PetApp.application.mentor.dto.MentorChatResponse;
import com.jf.PetApp.application.mentor.dto.MentorTurnDTO;
import com.jf.PetApp.application.mentor.port.MentorChatPort;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.application.mentor.port.MentorMessageRepositoryPort;
import com.jf.PetApp.application.pet.usecase.GetMyPetUseCase;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.MentorConversation;
import com.jf.PetApp.core.domain.MentorMessage;
import com.jf.PetApp.core.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GetMentorReplyUseCaseImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    @Mock
    private GetPortfolioAllocationUseCase getPortfolioAllocationUseCase;
    @Mock
    private GetMyPetUseCase getMyPetUseCase;
    @Mock
    private GetLearningProgressUseCase getLearningProgressUseCase;
    @Mock
    private GetAcademyCatalogUseCase getAcademyCatalogUseCase;
    @Mock
    private MentorChatPort mentorChatPort;
    @Mock
    private MentorConversationRepositoryPort conversationRepositoryPort;
    @Mock
    private MentorMessageRepositoryPort messageRepositoryPort;

    private GetMentorReplyUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final Long CONVERSATION_ID = 42L;
    private static final PortfolioSummaryDTO EMPTY_SUMMARY =
            new PortfolioSummaryDTO(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 0);
    private static final LearningProgressResult EMPTY_LEARNING_PROGRESS =
            new LearningProgressResult(Set.of(), Set.of(), Set.of(), 0, 1, 0, 50);
    private static final AcademyCatalogResult EMPTY_ACADEMY_CATALOG =
            new AcademyCatalogResult(List.of(), List.of(), List.of(), List.of());

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetMentorReplyUseCaseImpl(
                userRepository, getPortfolioSummaryUseCase, getPortfolioAllocationUseCase, getMyPetUseCase,
                getLearningProgressUseCase, getAcademyCatalogUseCase,
                mentorChatPort, conversationRepositoryPort, messageRepositoryPort);

        User user = new User();
        user.setEmail(EMAIL);
        user.setPreferredLanguage("pt");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(getPortfolioSummaryUseCase.execute(EMAIL)).thenReturn(EMPTY_SUMMARY);
        when(getPortfolioAllocationUseCase.execute(EMAIL)).thenReturn(List.of());
        when(getMyPetUseCase.execute(EMAIL)).thenReturn(Optional.empty());
        when(getLearningProgressUseCase.execute(EMAIL)).thenReturn(EMPTY_LEARNING_PROGRESS);
        when(getAcademyCatalogUseCase.execute(anyString())).thenReturn(EMPTY_ACADEMY_CATALOG);

        MentorConversation existingConversation =
                new MentorConversation(CONVERSATION_ID, EMAIL, "Existing chat", Instant.now(), Instant.now());
        when(conversationRepositoryPort.findByIdAndUser(eq(CONVERSATION_ID), eq(EMAIL)))
                .thenReturn(Optional.of(existingConversation));
        when(messageRepositoryPort.findRecentByConversation(anyLong(), anyInt())).thenReturn(List.of());
    }

    private MentorChatRequest requestWithConversation(Long conversationId) {
        return new MentorChatRequest("What are dividends?", conversationId, null);
    }

    @Test
    void execute_WhenUserDoesNotExist_Throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID)));
    }

    @Test
    void execute_WhenConversationIdDoesNotBelongToUser_Throws() {
        when(conversationRepositoryPort.findByIdAndUser(eq(CONVERSATION_ID), eq(EMAIL)))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID)));
    }

    @Test
    void execute_WithNoConversationId_CreatesANewConversation() {
        MentorConversation created = new MentorConversation(99L, EMAIL, null, Instant.now(), Instant.now());
        when(conversationRepositoryPort.create(eq(EMAIL), any())).thenReturn(created);
        when(mentorChatPort.generateReply(anyString(), any(), anyString())).thenReturn("Dividends are periodic payments...");

        MentorChatResponse response = useCase.execute(EMAIL, requestWithConversation(null));

        assertEquals(99L, response.conversationId());
        verify(conversationRepositoryPort).create(eq(EMAIL), any());
    }

    @Test
    void execute_OnSuccess_ReturnsTheMentorReplyVerbatimAndPersistsBothTurns() {
        when(mentorChatPort.generateReply(anyString(), any(), anyString())).thenReturn("Dividends are periodic payments...");

        MentorChatResponse response = useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID));

        assertEquals("Dividends are periodic payments...", response.reply());
        verify(messageRepositoryPort).append(CONVERSATION_ID, "user", "What are dividends?");
        verify(messageRepositoryPort).append(CONVERSATION_ID, "mentor", "Dividends are periodic payments...");
    }

    @Test
    void execute_WhenMentorChatThrows_ReturnsTheCannedFallbackInsteadOfPropagating() {
        when(mentorChatPort.generateReply(anyString(), any(), anyString())).thenThrow(new RuntimeException("Provider request failed"));

        MentorChatResponse response = useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID));

        assertNotNull(response.reply());
        assertTrue(response.reply().toLowerCase().contains("trouble") || !response.reply().isBlank());
        verify(messageRepositoryPort).append(eq(CONVERSATION_ID), eq("mentor"), anyString());
    }

    @Test
    void execute_WithMoreThanTenHistoryTurns_RequestsOnlyTheMostRecentWindowFromStorage() {
        List<MentorMessage> recent = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            recent.add(new MentorMessage((long) i, CONVERSATION_ID, i % 2 == 0 ? "user" : "mentor", "turn " + i, Instant.now()));
        }
        when(messageRepositoryPort.findRecentByConversation(CONVERSATION_ID, 20)).thenReturn(recent);
        when(mentorChatPort.generateReply(anyString(), any(), anyString())).thenReturn("ok");

        useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MentorTurnDTO>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(mentorChatPort).generateReply(anyString(), historyCaptor.capture(), anyString());

        assertEquals(20, historyCaptor.getValue().size());
        verify(messageRepositoryPort).findRecentByConversation(CONVERSATION_ID, 20);
    }

    @Test
    void execute_OnFirstMessage_AutoTitlesTheConversationFromIt() {
        MentorConversation untitled = new MentorConversation(CONVERSATION_ID, EMAIL, null, Instant.now(), Instant.now());
        when(conversationRepositoryPort.findByIdAndUser(eq(CONVERSATION_ID), eq(EMAIL)))
                .thenReturn(Optional.of(untitled));
        when(mentorChatPort.generateReply(anyString(), any(), anyString())).thenReturn("ok");

        MentorChatResponse response = useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID));

        assertEquals("What are dividends?", response.title());
        verify(conversationRepositoryPort).updateTitle(CONVERSATION_ID, "What are dividends?");
        verify(conversationRepositoryPort, never()).touch(any());
    }

    @Test
    void execute_OnSubsequentMessage_TouchesRatherThanRetitling() {
        when(mentorChatPort.generateReply(anyString(), any(), anyString())).thenReturn("ok");

        useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID));

        verify(conversationRepositoryPort).touch(CONVERSATION_ID);
        verify(conversationRepositoryPort, never()).updateTitle(any(), any());
    }

    @Test
    void execute_WhenMentorReplyViolatesSafetyRules_ReplacesItWithASafeRedirectInstead() {
        when(mentorChatPort.generateReply(anyString(), any(), anyString()))
                .thenReturn("You should buy PETR4 right now, it's a guaranteed win.");

        MentorChatResponse response = useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID));

        assertFalse(response.reply().contains("PETR4"));
        ArgumentCaptor<String> persistedReply = ArgumentCaptor.forClass(String.class);
        verify(messageRepositoryPort).append(eq(CONVERSATION_ID), eq("mentor"), persistedReply.capture());
        assertEquals(response.reply(), persistedReply.getValue());
    }

    @Test
    void execute_WhenMentorReplyIsSafe_PersistsAndReturnsItUnchanged() {
        when(mentorChatPort.generateReply(anyString(), any(), anyString()))
                .thenReturn("Diversification means spreading your money across different kinds of assets.");

        MentorChatResponse response = useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID));

        assertEquals("Diversification means spreading your money across different kinds of assets.", response.reply());
    }

    @Test
    void execute_PassesAcademyLevelAndNextLessonIntoTheSystemPrompt() {
        LearningProgressResult progress = new LearningProgressResult(Set.of(), Set.of(), Set.of(), 30, 2, 5, 50);
        AcademyDomainView domain = new AcademyDomainView("dom-1", "Investing", null, null, 1, List.of("school-1"));
        AcademySchoolView school = new AcademySchoolView("school-1", "dom-1", "Equities", null, null, 1, List.of(), true);
        AcademyModuleView module = new AcademyModuleView(
                "module-1", "school-1", "Stocks 101", null, null, "BEGINNER", 1,
                List.of("lesson-1"), List.of(), true);
        AcademyLessonView lesson = new AcademyLessonView(
                "lesson-1", "module-1", "What is a stock?", null, null, null, 1, 10, List.of(), List.of());
        AcademyCatalogResult catalog = new AcademyCatalogResult(
                List.of(domain), List.of(school), List.of(module), List.of(lesson));

        when(getLearningProgressUseCase.execute(EMAIL)).thenReturn(progress);
        when(getAcademyCatalogUseCase.execute(anyString())).thenReturn(catalog);
        when(mentorChatPort.generateReply(anyString(), any(), anyString())).thenReturn("ok");

        useCase.execute(EMAIL, requestWithConversation(CONVERSATION_ID));

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mentorChatPort).generateReply(systemPromptCaptor.capture(), any(), anyString());
        String systemPrompt = systemPromptCaptor.getValue();

        assertTrue(systemPrompt.contains("Academy progress: level 2 (5/50 XP into this level)"));
        assertTrue(systemPrompt.contains("Next lesson to continue: \"What is a stock?\" (module: Stocks 101)"));
    }
}
