package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        InvestmentJpaEntity entity = new InvestmentJpaEntity();
        UserJpaEntity user = new UserJpaEntity();
        LocalDate purchaseDate = LocalDate.of(2024, 1, 15);

        entity.setId(9);
        entity.setName("Apple Inc.");
        entity.setQuantity(10.5);
        entity.setPurchasePrice(150.25);
        entity.setPurchaseDate(purchaseDate);
        entity.setType(InvestmentType.STOCKS);
        entity.setUser(user);

        assertThat(entity.getId()).isEqualTo(9);
        assertThat(entity.getName()).isEqualTo("Apple Inc.");
        assertThat(entity.getQuantity()).isEqualTo(10.5);
        assertThat(entity.getPurchasePrice()).isEqualTo(150.25);
        assertThat(entity.getPurchaseDate()).isEqualTo(purchaseDate);
        assertThat(entity.getType()).isEqualTo(InvestmentType.STOCKS);
        assertThat(entity.getUser()).isSameAs(user);
    }
}
