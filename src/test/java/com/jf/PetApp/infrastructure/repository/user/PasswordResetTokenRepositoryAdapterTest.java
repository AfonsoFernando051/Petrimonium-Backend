package com.jf.PetApp.infrastructure.repository.user;

import com.jf.PetApp.application.auth.exception.PasswordResetTokenInvalidException;
import com.jf.PetApp.application.auth.port.PasswordResetTokenRepositoryPort;
import com.jf.PetApp.core.domain.PasswordResetToken;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class PasswordResetTokenRepositoryAdapterTest {

    @Autowired
    private PasswordResetTokenJpaRepository jpaRepository;

    @Autowired
    private SpringUserJpaRepository userJpaRepository;

    private PasswordResetTokenRepositoryPort adapter;

    private Long userId;

    @BeforeEach
    void setUp() {
        adapter = new PasswordResetTokenRepositoryAdapter(jpaRepository);

        User user = new User();
        user.setUsername("investor");
        user.setEmail("investor@test.com");
        user.setPassword("hash");
        UserJpaEntity saved = userJpaRepository.save(UserJpaEntity.fromDomain(user));
        userId = saved.toDomain().getId();
    }

    private PasswordResetToken tokenFor(Long userId, String hash) {
        Instant now = Instant.now();
        return new PasswordResetToken(null, userId, hash, now.plusSeconds(1800), null, now);
    }

    @Test
    void save_ThenFindByTokenHash_RoundTripsEveryField() {
        PasswordResetToken saved = adapter.save(tokenFor(userId, "hash-abc"));

        Optional<PasswordResetToken> found = adapter.findByTokenHash("hash-abc");

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(saved.id());
        assertThat(found.get().userId()).isEqualTo(userId);
        assertThat(found.get().tokenHash()).isEqualTo("hash-abc");
        assertThat(found.get().usedAt()).isNull();
    }

    @Test
    void findByTokenHash_WhenUnknown_ReturnsEmpty() {
        assertThat(adapter.findByTokenHash("unknown-hash")).isEmpty();
    }

    @Test
    void markUsed_SetsUsedAtOnThatToken() {
        PasswordResetToken saved = adapter.save(tokenFor(userId, "hash-abc"));
        Instant usedAt = Instant.now();

        adapter.markUsed(saved.id(), usedAt);

        Optional<PasswordResetToken> found = adapter.findByTokenHash("hash-abc");
        assertThat(found).isPresent();
        assertThat(found.get().usedAt()).isNotNull();
    }

    @Test
    void markUsed_ForUnknownTokenId_ThrowsPasswordResetTokenInvalidException() {
        assertThrows(PasswordResetTokenInvalidException.class, () -> adapter.markUsed(999L, Instant.now()));
    }

    @Test
    void invalidateOutstandingForUser_MarksEveryNotYetUsedTokenAsUsed() {
        adapter.save(tokenFor(userId, "hash-1"));
        adapter.save(tokenFor(userId, "hash-2"));

        adapter.invalidateOutstandingForUser(userId);

        assertThat(adapter.findByTokenHash("hash-1").get().usedAt()).isNotNull();
        assertThat(adapter.findByTokenHash("hash-2").get().usedAt()).isNotNull();
    }

    @Test
    void invalidateOutstandingForUser_DoesNotTouchAlreadyUsedTokensDifferently_AndIsolatedPerUser() {
        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("hash");
        Long otherUserId = userJpaRepository.save(UserJpaEntity.fromDomain(otherUser)).toDomain().getId();

        adapter.save(tokenFor(userId, "hash-mine"));
        adapter.save(tokenFor(otherUserId, "hash-other"));

        adapter.invalidateOutstandingForUser(userId);

        assertThat(adapter.findByTokenHash("hash-mine").get().usedAt()).isNotNull();
        assertThat(adapter.findByTokenHash("hash-other").get().usedAt()).isNull();
    }
}
