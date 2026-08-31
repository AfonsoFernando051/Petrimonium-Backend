package com.jf.PetApp.infrastructure.repository.gamification;

import java.time.Instant;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.core.domain.gamification.XpEventType;
import com.jf.PetApp.infrastructure.entity.XpEventJpaEntity;

@Repository
public interface XpEventJpaRepository extends JpaRepository<XpEventJpaEntity, Long> {
    boolean existsByUserIdAndEventTypeAndSourceId(Long userId, XpEventType eventType, String sourceId);

    @Query("select coalesce(sum(x.amount), 0) from XpEventJpaEntity x where x.userId = :userId")
    int sumAmountByUserId(@Param("userId") Long userId);

    int countByUserIdAndEventTypeAndCreatedAtBetween(
            Long userId, XpEventType eventType, Instant fromInclusive, Instant toExclusive);

    @Query("select x.sourceId from XpEventJpaEntity x where x.userId = :userId and x.eventType = :eventType")
    Set<String> sourceIdsByUserIdAndEventType(@Param("userId") Long userId, @Param("eventType") XpEventType eventType);

    void deleteByUserId(Long userId);
}
