package com.jf.PetApp.infrastructure.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link DotenvLoader} hardcodes its target as the relative path {@code .env}, resolved against
 * the process working directory (the module root when Surefire runs it) -- the very same file
 * this repo keeps for local secrets. Every test below backs that file up in {@code @BeforeEach}
 * and restores it byte-for-byte in {@code @AfterEach} (even on failure), and only ever asserts
 * on uniquely-generated property keys so a real developer's `.env` values are never touched.
 */
class DotenvLoaderTest {

    private static final Path ENV_PATH = Path.of(".env");

    private byte[] originalEnvBytes;
    private boolean originalEnvExisted;
    private final List<String> propertyKeysToClear = new ArrayList<>();

    @BeforeEach
    void backupExistingEnv() throws IOException {
        originalEnvExisted = Files.isRegularFile(ENV_PATH);
        if (originalEnvExisted) {
            originalEnvBytes = Files.readAllBytes(ENV_PATH);
        }
    }

    @AfterEach
    void restoreEnvAndClearProperties() throws IOException {
        if (Files.isRegularFile(ENV_PATH)) {
            // Permission test may have locked it down; restore write access before overwriting/removing.
            try {
                Set<PosixFilePermission> perms = EnumSet.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(ENV_PATH, perms);
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX filesystem (e.g. Windows) -- nothing to restore.
            }
        }
        if (originalEnvExisted) {
            Files.write(ENV_PATH, originalEnvBytes);
        } else {
            Files.deleteIfExists(ENV_PATH);
        }
        propertyKeysToClear.forEach(System::clearProperty);
        propertyKeysToClear.clear();
    }

    private String uniqueKey() {
        String key = "DOTENV_TEST_" + UUID.randomUUID().toString().replace("-", "_");
        propertyKeysToClear.add(key);
        return key;
    }

    @Test
    void loadIntoSystemProperties_WithNoEnvFilePresent_IsANoOp() throws IOException {
        Files.deleteIfExists(ENV_PATH);
        String key = uniqueKey();

        DotenvLoader.loadIntoSystemProperties();

        assertNull(System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_WithASimpleKeyValueLine_SetsTheSystemProperty() throws IOException {
        String key = uniqueKey();
        Files.writeString(ENV_PATH, key + "=some-value\n");

        DotenvLoader.loadIntoSystemProperties();

        assertEquals("some-value", System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_TrimsWhitespaceAroundKeyAndValue() throws IOException {
        String key = uniqueKey();
        Files.writeString(ENV_PATH, "  " + key + "   =   spaced-value  \n");

        DotenvLoader.loadIntoSystemProperties();

        assertEquals("spaced-value", System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_SkipsBlankLines() throws IOException {
        String key = uniqueKey();
        Files.writeString(ENV_PATH, "\n\n   \n" + key + "=value\n\n");

        DotenvLoader.loadIntoSystemProperties();

        assertEquals("value", System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_SkipsCommentLines() throws IOException {
        String key = uniqueKey();
        Files.writeString(ENV_PATH, "# this is a comment\n" + key + "=value\n# another comment\n");

        DotenvLoader.loadIntoSystemProperties();

        assertEquals("value", System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_SkipsLinesWithNoEqualsSign() throws IOException {
        String key = uniqueKey();
        Files.writeString(ENV_PATH, "not-a-valid-line\n" + key + "=value\n");

        DotenvLoader.loadIntoSystemProperties();

        assertEquals("value", System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_SkipsLinesWhereEqualsSignIsTheFirstCharacter() throws IOException {
        String key = uniqueKey();
        // "=value" has separatorIndex == 0, which the loader treats as having no key. Attempting
        // to look that up via System.getProperty("") would itself throw IllegalArgumentException,
        // so the absence of that crash (plus the sibling key still loading) is the assertion here.
        Files.writeString(ENV_PATH, "=orphan-value\n" + key + "=value\n");

        DotenvLoader.loadIntoSystemProperties();

        assertEquals("value", System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_AllowsAnEqualsSignWithinTheValue() throws IOException {
        String key = uniqueKey();
        Files.writeString(ENV_PATH, key + "=a=b=c\n");

        DotenvLoader.loadIntoSystemProperties();

        assertEquals("a=b=c", System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_SkipsKeysWithABlankValue() throws IOException {
        // "KEY=" (blank) is the documented .env.example convention for "leave this disabled" —
        // it must NOT become a present-but-empty system property, since some Spring Boot
        // auto-configuration (e.g. MailSenderAutoConfiguration, keyed on spring.mail.host) only
        // checks property *presence*, not blankness, and would otherwise wrongly activate.
        String key = uniqueKey();
        Files.writeString(ENV_PATH, key + "=\n");

        DotenvLoader.loadIntoSystemProperties();

        assertNull(System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_SkipsAKeyWithOnlyWhitespaceAsItsValue() throws IOException {
        String key = uniqueKey();
        Files.writeString(ENV_PATH, key + "=   \n");

        DotenvLoader.loadIntoSystemProperties();

        assertNull(System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_NeverOverridesAPropertyAlreadySet() throws IOException {
        String key = uniqueKey();
        System.setProperty(key, "pre-existing-value");
        Files.writeString(ENV_PATH, key + "=from-dotenv\n");

        DotenvLoader.loadIntoSystemProperties();

        assertEquals("pre-existing-value", System.getProperty(key));
    }

    @Test
    void loadIntoSystemProperties_WithMultipleKeys_SetsEachOne() throws IOException {
        String key1 = uniqueKey();
        String key2 = uniqueKey();
        Files.writeString(ENV_PATH, key1 + "=value1\n" + key2 + "=value2\n");

        DotenvLoader.loadIntoSystemProperties();

        assertEquals("value1", System.getProperty(key1));
        assertEquals("value2", System.getProperty(key2));
    }

    @Test
    void loadIntoSystemProperties_WhenTheFileCannotBeRead_DoesNotThrow() throws IOException {
        String key = uniqueKey();
        Files.writeString(ENV_PATH, key + "=value\n");
        try {
            Files.setPosixFilePermissions(ENV_PATH, EnumSet.noneOf(PosixFilePermission.class));
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem: permission-denial can't be simulated this way, skip assertion.
            return;
        }

        DotenvLoader.loadIntoSystemProperties();

        assertNull(System.getProperty(key));
    }
}
