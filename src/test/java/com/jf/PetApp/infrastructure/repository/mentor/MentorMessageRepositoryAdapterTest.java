package com.jf.PetApp.infrastructure.repository.mentor;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.port.MentorMessageRepositoryPort;
import com.jf.PetApp.core.domain.MentorMessage;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.entity.MentorConversationJpaEntity;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class MentorMessageRepositoryAdapterTest {

    @Autowired
    private SpringMentorMessageJpaRepository messageJpaRepository;

    @Autowired
    private SpringMentorConversationJpaRepository conversationJpaRepository;

    @Autowired
    private SpringUserJpaRepository userJpaRepository;

    private MentorMessageRepositoryPort adapter;

    private Long conversationId;

    @BeforeEach
    void setUp() {
        adapter = new MentorMessageRepositoryAdapter(messageJpaRepository, conversationJpaRepository);

        User user = new User();
        user.setUsername("investor");
        user.setEmail("investor@test.com");
        user.setPassword("hash");
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.fromDomain(user));

        MentorConversationJpaEntity conversation = new MentorConversationJpaEntity();
        conversation.setUser(savedUser);
        conversation.setTitle("Conversation");
        Instant now = Instant.now();
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationId = conversationJpaRepository.save(conversation).getId();
    }

    @Test
    void append_ForUnknownConversation_ThrowsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> adapter.append(999L, "user", "Hello"));
    }

    @Test
    void append_PersistsMessageWithCreatedAtSet() {
        MentorMessage message = adapter.append(conversationId, "user", "Hello there");

        assertThat(message.id()).isNotNull();
        assertThat(message.conversationId()).isEqualTo(conversationId);
        assertThat(message.role()).isEqualTo("user");
        assertThat(message.text()).isEqualTo("Hello there");
        assertThat(message.createdAt()).isNotNull();
    }

    @Test
    void findAllByConversation_ReturnsMessagesInChronologicalOrder() {
        MentorMessage first = adapter.append(conversationId, "user", "First");
        MentorMessage second = adapter.append(conversationId, "mentor", "Second");

        List<MentorMessage> found = adapter.findAllByConversation(conversationId);

        assertThat(found).extracting(MentorMessage::id).containsExactly(first.id(), second.id());
    }

    @Test
    void findAllByConversation_ForConversationWithNoMessages_ReturnsEmptyList() {
        assertThat(adapter.findAllByConversation(conversationId)).isEmpty();
    }

    @Test
    void findRecentByConversation_ReturnsAtMostLimitMessagesInChronologicalOrder() {
        MentorMessage m1 = adapter.append(conversationId, "user", "1");
        MentorMessage m2 = adapter.append(conversationId, "mentor", "2");
        MentorMessage m3 = adapter.append(conversationId, "user", "3");

        List<MentorMessage> recent = adapter.findRecentByConversation(conversationId, 2);

        assertThat(recent).extracting(MentorMessage::id).containsExactly(m2.id(), m3.id());
        assertThat(m1).isNotNull();
    }

    @Test
    void findRecentByConversation_WhenFewerMessagesThanLimit_ReturnsAllOfThem() {
        MentorMessage only = adapter.append(conversationId, "user", "only one");

        List<MentorMessage> recent = adapter.findRecentByConversation(conversationId, 10);

        assertThat(recent).extracting(MentorMessage::id).containsExactly(only.id());
    }
}
