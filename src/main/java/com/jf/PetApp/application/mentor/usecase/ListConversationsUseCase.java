package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.mentor.dto.ConversationSummaryDTO;

import java.util.List;

public interface ListConversationsUseCase {
    List<ConversationSummaryDTO> execute(String email);
}
