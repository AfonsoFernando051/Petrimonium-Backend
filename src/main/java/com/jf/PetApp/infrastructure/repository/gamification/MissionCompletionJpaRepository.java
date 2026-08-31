package com.jf.PetApp.infrastructure.repository.gamification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.MissionCompletionJpaEntity;

@Repository
public interface MissionCompletionJpaRepository extends JpaRepository<MissionCompletionJpaEntity, Long> {

    boolean existsByUserIdAndMissionCodeAndPeriodKey(Long userId, String missionCode, String periodKey);

    @Query("select coalesce(sum(m.xpAwarded), 0) from MissionCompletionJpaEntity m where m.userId = :userId")
    int sumXpAwardedByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
