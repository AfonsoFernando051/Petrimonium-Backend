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
@Table(name = "mission_completions", schema = "gamification")
@Getter
@Setter
public class MissionCompletionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "mission_code")
    private String missionCode;

    @Column(name = "period_key")
    private String periodKey;

    @Column(name = "xp_awarded")
    private int xpAwarded;

    @Column(name = "completed_at")
    private Instant completedAt;
}
