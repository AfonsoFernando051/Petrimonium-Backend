package com.jf.PetApp.application.mentor.dto;

import java.util.List;

public record ConversationDetailDTO(Long id, String title, List<MentorMessageDTO> messages) {
}
