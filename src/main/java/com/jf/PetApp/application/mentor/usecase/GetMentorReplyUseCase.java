package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.mentor.dto.MentorChatRequest;
import com.jf.PetApp.application.mentor.dto.MentorChatResponse;

public interface GetMentorReplyUseCase {
    MentorChatResponse execute(String email, MentorChatRequest request);
}
