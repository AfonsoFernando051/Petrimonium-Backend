package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "jf_investments", schema = "real_portfolio")
@Getter
@Setter
public class InvestmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "purchase_price", precision = 19, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "purchase_date")
    private java.time.LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    private InvestmentType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Detects two concurrent edits of the same lot (e.g. two devices) — without it, whichever
     * request flushes last silently overwrites the other with no signal to either caller. See
     * V35__investment_optimistic_lock.sql.
     */
    @Version
    private Integer version;
}
