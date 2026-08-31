package com.jf.PetApp.application.mentor.usecase;

public interface DeleteConversationUseCase {
    void execute(String email, Long conversationId);
}
