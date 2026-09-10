package com.jf.PetApp.infrastructure.entity;

import java.time.Instant;

import com.jf.PetApp.core.domain.gamification.XpEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "xp_events", schema = "gamification")
@Getter
@Setter
public class XpEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private XpEventType eventType;

    private int amount;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "created_at")
    private Instant createdAt;
}
