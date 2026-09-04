package com.jf.PetApp.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jf.PetApp.application.mentor.exception.MentorDisabledException;

import org.junit.jupiter.api.Test;

class MentorKillSwitchTest {

    @Test
    void enabledByDefault_LetsProviderPathsThrough() {
        MentorKillSwitch killSwitch = new MentorKillSwitch(true);

        assertTrue(killSwitch.isEnabled());
        assertDoesNotThrow(killSwitch::assertEnabled);
    }

    @Test
    void whenDisabled_RefusesProviderPaths() {
        MentorKillSwitch killSwitch = new MentorKillSwitch(false);

        assertFalse(killSwitch.isEnabled());
        assertThrows(MentorDisabledException.class, killSwitch::assertEnabled);
    }
}
