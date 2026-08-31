package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.mentor.dto.ConversationDetailDTO;

public interface GetConversationUseCase {
    ConversationDetailDTO execute(String email, Long conversationId);
}
