package com.jf.PetApp.infrastructure.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "achievement_unlocks", schema = "gamification")
@Getter
@Setter
public class AchievementUnlockJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "achievement_code")
    private String achievementCode;

    @Column(name = "xp_awarded")
    private int xpAwarded;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;
}
