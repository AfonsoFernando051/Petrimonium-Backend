package com.jf.PetApp.infrastructure.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails startup loudly if neither {@code dev} nor {@code prod} is an active Spring profile.
 *
 * Why this exists: {@code application.properties} (the always-loaded base config) carries no
 * datasource of its own — {@code dev}/{@code prod} each supply their own — specifically so a
 * deployment that forgets to set the profile can't silently inherit dev-permissive settings
 * (see that file's header comment). The gap this guard closes: with no explicit datasource
 * configured and H2 on the classpath, Spring Boot's own {@code DataSourceAutoConfiguration}
 * quietly falls back to an embedded, unseeded H2 database rather than failing to start — the
 * app comes up looking healthy, but `db/migration-dev`'s seed users were never loaded, so
 * every login fails with "invalid credentials" and nothing in the logs points at why. This
 * bean turns that silent, confusing failure into an immediate, explicit one.
 */
@Component
public class ActiveProfileGuard implements InitializingBean {

    private final Environment environment;

    public ActiveProfileGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        // "test" is accepted too — src/test/resources/application.properties (a separate
        // classpath resource that fully shadows this one during `mvn test`) sets
        // spring.profiles.active=test precisely so full-context tests satisfy this guard
        // the same explicit way dev/prod do, rather than being silently exempt from it.
        boolean hasKnownProfile = environment.acceptsProfiles(
                org.springframework.core.env.Profiles.of("dev", "prod", "test"));
        if (!hasKnownProfile) {
            throw new IllegalStateException(
                    "No active Spring profile ('dev' or 'prod') — refusing to start. "
                    + "Local development: set spring.profiles.active=dev in your .env file "
                    + "(see .env.example). Production: set SPRING_PROFILES_ACTIVE=prod "
                    + "(already the case in the shipped Dockerfile — this only fires if the "
                    + "app is run outside of it without setting the profile explicitly).");
        }
    }
}
