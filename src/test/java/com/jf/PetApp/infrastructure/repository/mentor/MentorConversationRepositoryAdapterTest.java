package com.jf.PetApp.infrastructure.repository.mentor;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.core.domain.MentorConversation;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class MentorConversationRepositoryAdapterTest {

    @Autowired
    private SpringMentorConversationJpaRepository conversationJpaRepository;

    @Autowired
    private SpringUserJpaRepository userJpaRepository;

    private MentorConversationRepositoryPort adapter;

    private String userEmail;

    @BeforeEach
    void setUp() {
        adapter = new MentorConversationRepositoryAdapter(conversationJpaRepository, userJpaRepository);

        User user = new User();
        user.setUsername("investor");
        user.setEmail("investor@test.com");
        user.setPassword("hash");
        userJpaRepository.save(UserJpaEntity.fromDomain(user));
        userEmail = "investor@test.com";
    }

    @Test
    void create_ForUnknownUser_ThrowsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> adapter.create("unknown@test.com", "Title", "wallet"));
    }

    @Test
    void create_PersistsConversationWithCreatedAndUpdatedTimestampsAndAppContextSet() {
        MentorConversation created = adapter.create(userEmail, "My Title", "wallet");

        assertThat(created.id()).isNotNull();
        assertThat(created.userEmail()).isEqualTo(userEmail);
        assertThat(created.title()).isEqualTo("My Title");
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNotNull();
        assertThat(created.appContext()).isEqualTo("wallet");
    }

    @Test
    void findAllByUser_ReturnsOnlyThatUsersConversationsOrderedByMostRecentlyUpdated() throws InterruptedException {
        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("hash");
        userJpaRepository.save(UserJpaEntity.fromDomain(otherUser));

        MentorConversation first = adapter.create(userEmail, "First", "wallet");
        Thread.sleep(5);
        MentorConversation second = adapter.create(userEmail, "Second", "wallet");
        adapter.create("other@test.com", "Other user's conversation", "wallet");

        List<MentorConversation> found = adapter.findAllByUser(userEmail, "wallet");

        assertThat(found).extracting(MentorConversation::id).containsExactly(second.id(), first.id());
        assertThat(found).allMatch(c -> c.userEmail().equals(userEmail));
    }

    @Test
    void findAllByUser_NeverReturnsAnotherAppContextsConversations() {
        adapter.create(userEmail, "Wallet chat", "wallet");
        adapter.create(userEmail, "Academy chat", "academy");

        List<MentorConversation> walletOnly = adapter.findAllByUser(userEmail, "wallet");
        List<MentorConversation> academyOnly = adapter.findAllByUser(userEmail, "academy");

        assertThat(walletOnly).extracting(MentorConversation::title).containsExactly("Wallet chat");
        assertThat(academyOnly).extracting(MentorConversation::title).containsExactly("Academy chat");
    }

    @Test
    void findByIdAndUser_WhenConversationBelongsToAnotherUser_ReturnsEmpty() {
        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("hash");
        userJpaRepository.save(UserJpaEntity.fromDomain(otherUser));

        MentorConversation created = adapter.create(userEmail, "Mine", "wallet");

        assertThat(adapter.findByIdAndUser(created.id(), "other@test.com", "wallet")).isEmpty();
        assertThat(adapter.findByIdAndUser(created.id(), userEmail, "wallet")).isPresent();
    }

    @Test
    void findByIdAndUser_WhenConversationBelongsToADifferentAppContext_ReturnsEmpty() {
        MentorConversation walletConversation = adapter.create(userEmail, "Wallet chat", "wallet");

        assertThat(adapter.findByIdAndUser(walletConversation.id(), userEmail, "academy")).isEmpty();
        assertThat(adapter.findByIdAndUser(walletConversation.id(), userEmail, "wallet")).isPresent();
    }

    @Test
    void updateTitle_ChangesTitleAndBumpsUpdatedAt() {
        MentorConversation created = adapter.create(userEmail, "Old Title", "wallet");

        adapter.updateTitle(created.id(), userEmail, "wallet", "New Title");

        Optional<MentorConversation> found = adapter.findByIdAndUser(created.id(), userEmail, "wallet");
        assertThat(found).isPresent();
        assertThat(found.get().title()).isEqualTo("New Title");
    }

    @Test
    void updateTitle_ForUnknownId_ThrowsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class,
                () -> adapter.updateTitle(999L, userEmail, "wallet", "Title"));
    }

    @Test
    void touch_UpdatesUpdatedAtTimestamp() throws InterruptedException {
        MentorConversation created = adapter.create(userEmail, "Title", "wallet");
        Thread.sleep(5);

        adapter.touch(created.id(), userEmail, "wallet");

        Optional<MentorConversation> found = adapter.findByIdAndUser(created.id(), userEmail, "wallet");
        assertThat(found).isPresent();
        assertThat(found.get().updatedAt()).isAfter(created.updatedAt());
    }

    @Test
    void touch_ForUnknownId_ThrowsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> adapter.touch(999L, userEmail, "wallet"));
    }

    @Test
    void delete_RemovesConversation() {
        MentorConversation created = adapter.create(userEmail, "Title", "wallet");

        adapter.delete(created.id(), userEmail, "wallet");

        assertThat(adapter.findByIdAndUser(created.id(), userEmail, "wallet")).isEmpty();
    }

    // ------------------------------------------------- DEM-71: writes are owner-scoped

    /**
     * The reason the mutations stopped taking a bare id. Ownership used to be the caller's job;
     * now a write addressed to someone else's conversation finds nothing, and reports it as
     * missing rather than forbidden so an id can't be used to probe for another user's data.
     */
    @Test
    void updateTitle_ForAnotherUsersConversation_ThrowsAndChangesNothing() {
        MentorConversation mine = adapter.create(userEmail, "Old Title", "wallet");

        assertThrows(ResourceNotFoundException.class,
                () -> adapter.updateTitle(mine.id(), "someone-else@test.com", "wallet", "Hacked"));

        assertThat(adapter.findByIdAndUser(mine.id(), userEmail, "wallet").orElseThrow().title())
                .isEqualTo("Old Title");
    }

    @Test
    void delete_ForAnotherUsersConversation_ThrowsAndKeepsTheConversation() {
        MentorConversation mine = adapter.create(userEmail, "Title", "wallet");

        assertThrows(ResourceNotFoundException.class,
                () -> adapter.delete(mine.id(), "someone-else@test.com", "wallet"));

        assertThat(adapter.findByIdAndUser(mine.id(), userEmail, "wallet")).isPresent();
    }

    /** The app_context half of the same scope — a Wallet id is not writable from an Academy session. */
    @Test
    void updateTitle_FromAnotherAppContext_ThrowsAndChangesNothing() {
        MentorConversation walletConversation = adapter.create(userEmail, "Old Title", "wallet");

        assertThrows(ResourceNotFoundException.class,
                () -> adapter.updateTitle(walletConversation.id(), userEmail, "academy", "Crossed over"));

        assertThat(adapter.findByIdAndUser(walletConversation.id(), userEmail, "wallet").orElseThrow().title())
                .isEqualTo("Old Title");
    }
}
