package com.jf.PetApp.application.mentor.port;

import com.jf.PetApp.application.mentor.dto.MentorTurnDTO;

import java.util.List;

public interface MentorChatPort {
    String generateReply(String systemPrompt, List<MentorTurnDTO> history, String userMessage);
}
