package com.jf.PetApp.application.mentor.exception;

import com.jf.PetApp.core.domain.enums.AppContextEnum;

/**
 * Raised when a Mentor request arrives in an {@link AppContextEnum} that has no system prompt of
 * its own yet.
 *
 * <p>Fails the request on purpose. The alternative — falling through to the Wallet prompt, which is
 * what the code used to do for anything that wasn't Academy — would answer, say, a Health session
 * using the user's real portfolio: a context leak dressed up as a working feature. A loud 4xx is
 * the honest outcome until {@code MentorSystemPromptBuilder} learns to speak for that context.
 */
public class UnsupportedMentorContextException extends RuntimeException {

    private final AppContextEnum appContext;

    public UnsupportedMentorContextException(AppContextEnum appContext) {
        super("The Mentor has no system prompt for app context "
                + (appContext == null ? "<none>" : appContext.name())
                + "; refusing to answer with another context's data.");
        this.appContext = appContext;
    }

    public AppContextEnum appContext() {
        return appContext;
    }
}
