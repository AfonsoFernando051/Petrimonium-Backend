package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        InvestmentJpaEntity entity = new InvestmentJpaEntity();
        UserJpaEntity user = new UserJpaEntity();
        LocalDate purchaseDate = LocalDate.of(2024, 1, 15);
        Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");
        Instant updatedAt = Instant.parse("2024-01-16T11:00:00Z");

        entity.setId(9);
        entity.setName("Apple Inc.");
        entity.setQuantity(BigDecimal.valueOf(10.5));
        entity.setPurchasePrice(BigDecimal.valueOf(150.25));
        entity.setPurchaseDate(purchaseDate);
        entity.setType(InvestmentType.STOCKS);
        entity.setUser(user);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        assertThat(entity.getId()).isEqualTo(9);
        assertThat(entity.getName()).isEqualTo("Apple Inc.");
        assertThat(entity.getQuantity()).isEqualTo(BigDecimal.valueOf(10.5));
        assertThat(entity.getPurchasePrice()).isEqualTo(BigDecimal.valueOf(150.25));
        assertThat(entity.getPurchaseDate()).isEqualTo(purchaseDate);
        assertThat(entity.getType()).isEqualTo(InvestmentType.STOCKS);
        assertThat(entity.getUser()).isSameAs(user);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
