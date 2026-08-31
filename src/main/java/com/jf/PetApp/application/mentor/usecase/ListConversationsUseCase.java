package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.mentor.dto.ConversationSummaryDTO;
import com.jf.PetApp.core.domain.enums.AppContextEnum;

import java.util.List;

public interface ListConversationsUseCase {
    List<ConversationSummaryDTO> execute(String email, AppContextEnum appContext);
}
