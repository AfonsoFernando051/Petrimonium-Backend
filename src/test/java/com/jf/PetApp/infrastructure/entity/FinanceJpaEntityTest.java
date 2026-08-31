package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.Finance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceJpaEntityTest {

    @Test
    void fromDomain_MapsIdAndBalance() {
        Finance finance = new Finance();
        finance.setId(5);
        finance.setBalance(new BigDecimal("123.45"));

        FinanceJpaEntity entity = FinanceJpaEntity.fromDomain(finance, new UserJpaEntity());

        assertThat(entity.toDomain().getId()).isEqualTo(5);
        assertThat(entity.toDomain().getBalance()).isEqualByComparingTo("123.45");
    }

    @Test
    void toDomain_MapsIdAndBalanceButNotInvestments() {
        Finance finance = new Finance();
        finance.setId(7);
        finance.setBalance(new BigDecimal("50.00"));
        FinanceJpaEntity entity = FinanceJpaEntity.fromDomain(finance, new UserJpaEntity());

        Finance result = entity.toDomain();

        assertThat(result.getId()).isEqualTo(7);
        assertThat(result.getBalance()).isEqualByComparingTo("50.00");
        assertThat(result.getInvestments()).isNull();
    }
}
