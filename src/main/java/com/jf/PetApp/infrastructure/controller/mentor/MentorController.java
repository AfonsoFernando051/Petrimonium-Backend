package com.jf.PetApp.infrastructure.controller.mentor;

import com.jf.PetApp.application.mentor.dto.ConversationDetailDTO;
import com.jf.PetApp.application.mentor.dto.ConversationSummaryDTO;
import com.jf.PetApp.application.mentor.dto.MentorChatRequest;
import com.jf.PetApp.application.mentor.dto.MentorChatResponse;
import com.jf.PetApp.application.mentor.dto.MentorSuggestionsResponse;
import com.jf.PetApp.application.mentor.dto.RenameConversationRequest;
import com.jf.PetApp.application.mentor.usecase.DeleteConversationUseCase;
import com.jf.PetApp.application.mentor.usecase.GetConversationUseCase;
import com.jf.PetApp.application.mentor.usecase.GetMentorReplyUseCase;
import com.jf.PetApp.application.mentor.usecase.ListConversationsUseCase;
import com.jf.PetApp.application.mentor.usecase.MentorPromptSuggestionsService;
import com.jf.PetApp.application.mentor.usecase.RenameConversationUseCase;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mentor")
public class MentorController {

    private final GetMentorReplyUseCase getMentorReplyUseCase;
    private final ListConversationsUseCase listConversationsUseCase;
    private final GetConversationUseCase getConversationUseCase;
    private final RenameConversationUseCase renameConversationUseCase;
    private final DeleteConversationUseCase deleteConversationUseCase;
    private final MentorPromptSuggestionsService mentorPromptSuggestionsService;

    public MentorController(GetMentorReplyUseCase getMentorReplyUseCase,
                             ListConversationsUseCase listConversationsUseCase,
                             GetConversationUseCase getConversationUseCase,
                             RenameConversationUseCase renameConversationUseCase,
                             DeleteConversationUseCase deleteConversationUseCase,
                             MentorPromptSuggestionsService mentorPromptSuggestionsService) {
        this.getMentorReplyUseCase = getMentorReplyUseCase;
        this.listConversationsUseCase = listConversationsUseCase;
        this.getConversationUseCase = getConversationUseCase;
        this.renameConversationUseCase = renameConversationUseCase;
        this.deleteConversationUseCase = deleteConversationUseCase;
        this.mentorPromptSuggestionsService = mentorPromptSuggestionsService;
    }

    @GetMapping("/suggestions")
    public ResponseEntity<MentorSuggestionsResponse> suggestions(
            @RequestParam(defaultValue = "pt") String language,
            @RequestParam(defaultValue = "5") int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 8));
        return ResponseEntity.ok(new MentorSuggestionsResponse(
                mentorPromptSuggestionsService.getRandomSuggestions(language, boundedLimit)));
    }

    @PostMapping("/chat")
    public ResponseEntity<MentorChatResponse> chat(@Valid @RequestBody MentorChatRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        AppContextEnum appContext = SecurityUtils.getCurrentAppContext().orElse(null);
        MentorChatResponse response = getMentorReplyUseCase.execute(email, request, appContext);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDTO>> listConversations() {
        String email = SecurityUtils.getCurrentUserEmail();
        AppContextEnum appContext = SecurityUtils.getCurrentAppContext().orElse(null);
        return ResponseEntity.ok(listConversationsUseCase.execute(email, appContext));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationDetailDTO> getConversation(@PathVariable Long id) {
        String email = SecurityUtils.getCurrentUserEmail();
        AppContextEnum appContext = SecurityUtils.getCurrentAppContext().orElse(null);
        return ResponseEntity.ok(getConversationUseCase.execute(email, id, appContext));
    }

    @PatchMapping("/conversations/{id}")
    public ResponseEntity<Void> renameConversation(@PathVariable Long id,
                                                     @Valid @RequestBody RenameConversationRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        AppContextEnum appContext = SecurityUtils.getCurrentAppContext().orElse(null);
        renameConversationUseCase.execute(email, id, request.title(), appContext);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        String email = SecurityUtils.getCurrentUserEmail();
        AppContextEnum appContext = SecurityUtils.getCurrentAppContext().orElse(null);
        deleteConversationUseCase.execute(email, id, appContext);
        return ResponseEntity.noContent().build();
    }
}
