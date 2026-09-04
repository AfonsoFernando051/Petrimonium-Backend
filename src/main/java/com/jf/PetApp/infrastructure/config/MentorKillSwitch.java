package com.jf.PetApp.infrastructure.config;

import com.jf.PetApp.application.mentor.exception.MentorDisabledException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The operational off switch for everything that reaches an LLM provider.
 *
 * <p>Exists so that a misbehaving or expensive Mentor can be stopped without a deploy — flip
 * {@code app.mentor.enabled} to {@code false} and restart, the same shape as
 * {@code app.b3-sync.enabled}. Until this existed there was no way to turn the AI off at all
 * short of pulling the API keys, which fails as an opaque provider error instead of an
 * intentional state.
 *
 * <p>Defaults to {@code true}: the Mentor is a core product surface, so an environment that
 * forgets to set this should get a working Mentor, not a silently dead one. That is the opposite
 * of the {@code app.b3-sync.enabled} default, which guards an integration that does not exist yet.
 */
@Component
public class MentorKillSwitch {

    private static final Logger log = LoggerFactory.getLogger(MentorKillSwitch.class);

    private final boolean enabled;

    public MentorKillSwitch(@Value("${app.mentor.enabled:true}") boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            log.warn("app.mentor.enabled=false — the Mentor's AI paths are switched off; "
                    + "/api/mentor/chat and /api/mentor/suggestions will refuse requests.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Guards a path that reaches the LLM provider. Reading, renaming and deleting existing
     * conversations deliberately do NOT call this: that is stored data the user already owns, and
     * turning off the AI should not take their history away with it.
     */
    public void assertEnabled() {
        if (!enabled) {
            throw new MentorDisabledException();
        }
    }
}
