package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.core.domain.enums.AppContextEnum;

public interface RenameConversationUseCase {
    void execute(String email, Long conversationId, String title, AppContextEnum appContext);
}
