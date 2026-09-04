package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.application.academy.dto.AcademyLessonView;
import com.jf.PetApp.application.academy.dto.AcademyModuleView;
import com.jf.PetApp.application.academy.service.AcademyNextLessonResolver;
import com.jf.PetApp.application.academy.usecase.GetAcademyCatalogUseCase;
import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;
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
import com.jf.PetApp.application.mentor.prompt.MentorSystemPromptBuilder;
import com.jf.PetApp.application.mentor.safety.MentorSafetyGuard;
import com.jf.PetApp.application.pet.usecase.GetMyPetUseCase;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPortfolioSummaryDTO;
import com.jf.PetApp.application.simulatedportfolio.usecase.GetSimulatedPortfolioUseCase;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.MentorConversation;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Stage 6 (Pet/XP/Mentor context separation): {@code appContext} decides which of two disjoint
 * data paths this call takes — {@link AppContextEnum#ACADEMY} pulls the simulated portfolio and
 * learning progress, everything else (including {@code null}, an unresolvable/legacy session)
 * takes the Wallet path and pulls the real portfolio. Whitelisting ACADEMY explicitly rather than
 * blacklisting WALLET means an ambiguous context can never accidentally surface Academy content.
 */
@Service
public class GetMentorReplyUseCaseImpl implements GetMentorReplyUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetMentorReplyUseCaseImpl.class);
    private static final int MAX_HISTORY_TURNS = 10;
    private static final int MAX_TITLE_LENGTH = 60;
    private static final String FALLBACK_REPLY =
            "Hmm, I'm having a little trouble thinking right now 🐾 Let's try again in a moment.";

    private final UserRepository userRepository;
    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final GetPortfolioAllocationUseCase getPortfolioAllocationUseCase;
    private final GetMyPetUseCase getMyPetUseCase;
    private final GetLearningProgressUseCase getLearningProgressUseCase;
    private final GetAcademyCatalogUseCase getAcademyCatalogUseCase;
    private final GetSimulatedPortfolioUseCase getSimulatedPortfolioUseCase;
    private final MentorChatPort mentorChatPort;
    private final MentorConversationRepositoryPort conversationRepositoryPort;
    private final MentorMessageRepositoryPort messageRepositoryPort;

    public GetMentorReplyUseCaseImpl(UserRepository userRepository,
                                      GetPortfolioSummaryUseCase getPortfolioSummaryUseCase,
                                      GetPortfolioAllocationUseCase getPortfolioAllocationUseCase,
                                      GetMyPetUseCase getMyPetUseCase,
                                      GetLearningProgressUseCase getLearningProgressUseCase,
                                      GetAcademyCatalogUseCase getAcademyCatalogUseCase,
                                      GetSimulatedPortfolioUseCase getSimulatedPortfolioUseCase,
                                      MentorChatPort mentorChatPort,
                                      MentorConversationRepositoryPort conversationRepositoryPort,
                                      MentorMessageRepositoryPort messageRepositoryPort) {
        this.userRepository = userRepository;
        this.getPortfolioSummaryUseCase = getPortfolioSummaryUseCase;
        this.getPortfolioAllocationUseCase = getPortfolioAllocationUseCase;
        this.getMyPetUseCase = getMyPetUseCase;
        this.getLearningProgressUseCase = getLearningProgressUseCase;
        this.getAcademyCatalogUseCase = getAcademyCatalogUseCase;
        this.getSimulatedPortfolioUseCase = getSimulatedPortfolioUseCase;
        this.mentorChatPort = mentorChatPort;
        this.conversationRepositoryPort = conversationRepositoryPort;
        this.messageRepositoryPort = messageRepositoryPort;
    }

    @Override
    public MentorChatResponse execute(String email, MentorChatRequest request, AppContextEnum appContext) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String appContextClaim = appContext == null ? null : appContext.claimValue();
        MentorConversation conversation = request.conversationId() != null
                ? conversationRepositoryPort.findByIdAndUser(request.conversationId(), email, appContextClaim)
                        .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"))
                : conversationRepositoryPort.create(email, null, appContextClaim);

        String language = MentorSystemPromptBuilder.resolveLanguage(request.context(), user.getPreferredLanguage());

        Pet pet = getMyPetUseCase.execute(email).orElse(null);
        boolean isAcademy = appContext == AppContextEnum.ACADEMY;
        String systemPrompt;
        List<String> sources;
        if (isAcademy) {
            systemPrompt = buildAcademyPrompt(email, pet, request, user, language);
            sources = List.of();
        } else {
            PortfolioSummaryDTO summary = getPortfolioSummaryUseCase.execute(email);
            List<AllocationSliceDTO> allocation = getPortfolioAllocationUseCase.execute(email);
            systemPrompt = MentorSystemPromptBuilder.buildForWallet(
                    pet, summary, allocation, request.context(), user.getPreferredLanguage());
            sources = MentorSystemPromptBuilder.walletSourcesFor(pet, summary, allocation, request.context());
        }

        List<MentorTurnDTO> history = messageRepositoryPort
                .findRecentByConversation(conversation.id(), MAX_HISTORY_TURNS * 2).stream()
                .map(m -> new MentorTurnDTO(m.role(), m.text()))
                .toList();

        String reply;
        try {
            reply = mentorChatPort.generateReply(systemPrompt, history, request.message());
            if (MentorSafetyGuard.violatesSafetyRules(reply)) {
                // Defense in depth behind the system prompt's own safety rules (see
                // MentorSafetyGuard's doc comment) — never let a flagged reply reach the user
                // or get persisted. Log that it happened, not the reply text itself.
                log.warn("Mentor reply flagged by MentorSafetyGuard and replaced with a safe redirect (user={})", email);
                reply = MentorSafetyGuard.safeRedirectReply(language);
            }
        } catch (Exception e) {
            // e.getMessage() here is safe to log: neither underlying client lets its API key
            // reach an exception message (see AnthropicChatClient/GeminiChatClient's own catch
            // blocks). Reachable only if both the primary provider and its Gemini fallback fail.
            log.warn("Mentor chat call failed (both providers), falling back to canned reply: {}", e.getMessage());
            reply = FALLBACK_REPLY;
        }

        messageRepositoryPort.append(conversation.id(), "user", request.message());
        messageRepositoryPort.append(conversation.id(), "mentor", reply);

        String title = conversation.title();
        if (title == null || title.isBlank()) {
            title = buildTitle(request.message());
            conversationRepositoryPort.updateTitle(conversation.id(), email, appContextClaim, title);
        } else {
            conversationRepositoryPort.touch(conversation.id(), email, appContextClaim);
        }

        return new MentorChatResponse(reply, conversation.id(), title, sources);
    }

    private String buildAcademyPrompt(String email, Pet pet, MentorChatRequest request, User user, String language) {
        SimulatedPortfolioSummaryDTO simulatedPortfolio = getSimulatedPortfolioUseCase.execute(email);

        LearningProgressResult learningProgress = getLearningProgressUseCase.execute(email);
        AcademyCatalogResult academyCatalog = getAcademyCatalogUseCase.execute(language);
        Optional<AcademyLessonView> nextLesson =
                AcademyNextLessonResolver.resolve(academyCatalog, learningProgress.completedLessonIds());
        String nextLessonTitle = nextLesson.map(AcademyLessonView::title).orElse(null);
        String nextModuleTitle = nextLesson
                .flatMap(lesson -> academyCatalog.modules().stream()
                        .filter(module -> module.id().equals(lesson.moduleId()))
                        .findFirst())
                .map(AcademyModuleView::title)
                .orElse(null);

        return MentorSystemPromptBuilder.buildForAcademy(
                pet, simulatedPortfolio, request.context(), user.getPreferredLanguage(),
                learningProgress, nextLessonTitle, nextModuleTitle);
    }

    private String buildTitle(String firstMessage) {
        String trimmed = firstMessage.strip();
        if (trimmed.length() <= MAX_TITLE_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_TITLE_LENGTH - 3) + "...";
    }
}
