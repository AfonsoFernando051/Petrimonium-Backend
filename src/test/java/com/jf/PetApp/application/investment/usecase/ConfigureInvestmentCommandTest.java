package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigureInvestmentCommandTest {

    @Test
    void constructor_RoundTripsAllFields() {
        LocalDate purchaseDate = LocalDate.of(2025, 3, 1);
        ConfigureInvestmentCommand command =
                new ConfigureInvestmentCommand("PETR4", 100.0, 30.5, purchaseDate, InvestmentType.STOCKS);

        assertThat(command.name()).isEqualTo("PETR4");
        assertThat(command.quantity()).isEqualTo(100.0);
        assertThat(command.purchasePrice()).isEqualTo(30.5);
        assertThat(command.purchaseDate()).isEqualTo(purchaseDate);
        assertThat(command.type()).isEqualTo(InvestmentType.STOCKS);
    }

    @Test
    void equals_AndHashCode_AreValueBased() {
        LocalDate purchaseDate = LocalDate.of(2025, 3, 1);
        ConfigureInvestmentCommand a =
                new ConfigureInvestmentCommand("PETR4", 100.0, 30.5, purchaseDate, InvestmentType.STOCKS);
        ConfigureInvestmentCommand b =
                new ConfigureInvestmentCommand("PETR4", 100.0, 30.5, purchaseDate, InvestmentType.STOCKS);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
