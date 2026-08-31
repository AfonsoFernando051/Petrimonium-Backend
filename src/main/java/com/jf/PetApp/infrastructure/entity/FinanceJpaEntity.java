package com.jf.PetApp.infrastructure.entity;

import java.math.BigDecimal;

import com.jf.PetApp.core.domain.Finance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "jf_finances")
public class FinanceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private BigDecimal balance;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserJpaEntity user;

    /* ---------- Mapping ---------- */

    public static FinanceJpaEntity fromDomain(Finance finance, UserJpaEntity user) {
        FinanceJpaEntity entity = new FinanceJpaEntity();
        entity.id = finance.getId();
        entity.balance = finance.getBalance();
        entity.user = user;
        return entity;
    }

    public Finance toDomain() {
        Finance finance = new Finance();
        finance.setId(id);
        finance.setBalance(balance);
        return finance;
    }
}
