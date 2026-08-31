package com.jf.PetApp.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ActiveProfileGuardTest {

    @Test
    void afterPropertiesSet_throws_whenNoProfileIsActive() {
        MockEnvironment environment = new MockEnvironment(); // no active profiles set
        ActiveProfileGuard guard = new ActiveProfileGuard(environment);

        assertThrows(IllegalStateException.class, guard::afterPropertiesSet);
    }

    @Test
    void afterPropertiesSet_doesNotThrow_whenDevProfileIsActive() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
        environment.setActiveProfiles("dev");
        ActiveProfileGuard guard = new ActiveProfileGuard(environment);

        assertDoesNotThrow(guard::afterPropertiesSet);
    }

    @Test
    void afterPropertiesSet_doesNotThrow_whenProdProfileIsActive() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ActiveProfileGuard guard = new ActiveProfileGuard(environment);

        assertDoesNotThrow(guard::afterPropertiesSet);
    }

    @Test
    void afterPropertiesSet_doesNotThrow_whenTestProfileIsActive() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        ActiveProfileGuard guard = new ActiveProfileGuard(environment);

        assertDoesNotThrow(guard::afterPropertiesSet);
    }

    @Test
    void afterPropertiesSet_throws_whenAnUnrelatedProfileIsActive() {
        // Guards against a typo (e.g. "devv") silently being treated as safe.
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");
        ActiveProfileGuard guard = new ActiveProfileGuard(environment);

        assertThrows(IllegalStateException.class, guard::afterPropertiesSet);
    }
}
