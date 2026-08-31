package com.jf.PetApp.infrastructure.repository.mentor;

import com.jf.PetApp.infrastructure.entity.MentorConversationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringMentorConversationJpaRepository extends JpaRepository<MentorConversationJpaEntity, Long> {

    List<MentorConversationJpaEntity> findByUser_EmailAndAppContextOrderByUpdatedAtDesc(String email, String appContext);

    Optional<MentorConversationJpaEntity> findByIdAndUser_EmailAndAppContext(Long id, String email, String appContext);

    /** Context-agnostic — every conversation a user has, regardless of app_context. Used by
     *  {@code DemoAccountResetAdapter} to wipe all of a demo account's Mentor history at once,
     *  not by any app-facing use case (those always filter by app_context). */
    List<MentorConversationJpaEntity> findByUser_Email(String email);

    // Relies on jf_mentor_messages' `on delete cascade` FK to jf_mentor_conversations
    // (see V11__mentor_conversations.sql) to clean up child messages.
    @Modifying
    @Query("delete from MentorConversationJpaEntity c where c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
