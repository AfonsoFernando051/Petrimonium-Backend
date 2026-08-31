package com.jf.PetApp.application.mentor.usecase;

public interface RenameConversationUseCase {
    void execute(String email, Long conversationId, String title);
}
