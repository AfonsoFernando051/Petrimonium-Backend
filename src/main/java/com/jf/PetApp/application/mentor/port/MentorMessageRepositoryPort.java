package com.jf.PetApp.application.mentor.port;

import com.jf.PetApp.core.domain.MentorMessage;

import java.util.List;

/**
 * Application-layer boundary for Mentor message persistence. See
 * {@link MentorConversationRepositoryPort}.
 */
public interface MentorMessageRepositoryPort {

    MentorMessage append(Long conversationId, String role, String text);

    List<MentorMessage> findAllByConversation(Long conversationId);

    /**
     * The most recent {@code limit} messages in the conversation, in
     * chronological (oldest-first) order — ready to feed straight into
     * Gemini as chat history.
     */
    List<MentorMessage> findRecentByConversation(Long conversationId, int limit);
}
