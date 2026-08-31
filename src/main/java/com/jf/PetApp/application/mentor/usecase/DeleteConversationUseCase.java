package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.core.domain.enums.AppContextEnum;

public interface DeleteConversationUseCase {
    void execute(String email, Long conversationId, AppContextEnum appContext);
}
