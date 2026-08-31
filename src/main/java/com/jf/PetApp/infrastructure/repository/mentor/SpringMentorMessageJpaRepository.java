package com.jf.PetApp.infrastructure.repository.mentor;

import com.jf.PetApp.infrastructure.entity.MentorMessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringMentorMessageJpaRepository extends JpaRepository<MentorMessageJpaEntity, Long> {

    List<MentorMessageJpaEntity> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);

    List<MentorMessageJpaEntity> findByConversation_IdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
}
