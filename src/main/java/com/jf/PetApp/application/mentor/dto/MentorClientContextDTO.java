package com.jf.PetApp.application.mentor.dto;

import jakarta.validation.constraints.Size;

/**
 * Signals only the mobile client knows (local pet preferences, current screen, UI language) —
 * everything else (portfolio, pet) is loaded server-side from the authenticated user. Bounds
 * mirror {@link MentorChatRequest#message()}'s size limit — this whole DTO ends up interpolated
 * into the LLM prompt, so every field needs a cap to bound prompt-injection surface and
 * prompt-cost inflation, not just the free-text message.
 */
public record MentorClientContextDTO(
    @Size(max = 200) String petGoal,
    @Size(max = 100) String investmentHorizon,
    @Size(max = 100) String currentScreen,
    @Size(max = 10) String language
) {
}
