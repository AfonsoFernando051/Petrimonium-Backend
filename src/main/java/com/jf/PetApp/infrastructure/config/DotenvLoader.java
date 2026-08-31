package com.jf.PetApp.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads a local, git-ignored `.env` file (KEY=VALUE per line, same syntax as
 * application.properties) into JVM system properties before Spring starts,
 * so secrets (Gemini/Brapi/LibreTranslate keys, JWT secret) never have to
 * live in application.properties or be committed to git — see .env.example
 * for the expected keys. System properties are always picked up by Spring's
 * Environment with high precedence, so `@Value("${api.gemini.key:}")` keeps
 * working unchanged.
 *
 * Deliberately independent of any Spring Boot bootstrap SPI (e.g. the
 * `EnvironmentPostProcessor` mechanism, deprecated-for-removal as of Boot
 * 4.0) — a plain call at the top of `main()` needs no `spring.factories`
 * registration and won't break across Boot version upgrades.
 *
 * No-op if `.env` isn't present (e.g. in CI/production, where real
 * environment variables are used instead — those are picked up by Spring's
 * normal relaxed binding regardless, with no code involvement here).
 *
 * A blank value (`KEY=`, the documented "leave blank to disable this"
 * convention throughout .env.example) is treated the same as the key being
 * entirely absent, not as the key being set to an empty string — the two
 * are NOT equivalent to Spring Boot's own auto-configuration: some
 * `@ConditionalOnProperty` checks (e.g. `MailSenderAutoConfiguration`, keyed
 * on `spring.mail.host`) only test whether the property is *present*, so a
 * blank-but-present value still activates a real bean (a `JavaMailSender`
 * pointed at an empty host, in that case) instead of leaving the intended
 * "not configured" fallback path reachable.
 */
public final class DotenvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotenvLoader.class);
    private static final String DOTENV_FILENAME = ".env";

    private DotenvLoader() {
    }

    public static void loadIntoSystemProperties() {
        Path dotenvPath = Path.of(DOTENV_FILENAME);
        if (!Files.isRegularFile(dotenvPath)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(dotenvPath)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separatorIndex).trim();
                String value = trimmed.substring(separatorIndex + 1).trim();
                if (value.isEmpty()) {
                    continue;
                }
                // Never override a real environment variable/-D flag already set.
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read .env: {}", e.getMessage());
        }
    }
}
