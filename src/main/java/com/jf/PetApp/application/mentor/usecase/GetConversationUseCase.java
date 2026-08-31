package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.mentor.dto.ConversationDetailDTO;
import com.jf.PetApp.core.domain.enums.AppContextEnum;

public interface GetConversationUseCase {
    ConversationDetailDTO execute(String email, Long conversationId, AppContextEnum appContext);
}
