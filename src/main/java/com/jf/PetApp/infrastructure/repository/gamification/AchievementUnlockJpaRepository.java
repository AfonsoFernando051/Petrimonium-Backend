package com.jf.PetApp.infrastructure.repository.gamification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AchievementUnlockJpaEntity;

@Repository
public interface AchievementUnlockJpaRepository extends JpaRepository<AchievementUnlockJpaEntity, Long> {

    boolean existsByUserIdAndAchievementCode(Long userId, String achievementCode);

    List<AchievementUnlockJpaEntity> findByUserId(Long userId);

    @Query("select coalesce(sum(a.xpAwarded), 0) from AchievementUnlockJpaEntity a where a.userId = :userId")
    int sumXpAwardedByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
