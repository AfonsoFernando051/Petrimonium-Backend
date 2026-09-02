package com.jf.PetApp.application.mentor.dto;

import java.util.List;

public record MentorChatResponse(String reply, Long conversationId, String title, List<String> sources) {
}
