package com.jf.PetApp.infrastructure.repository.investment;

import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.core.domain.Investment;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.InvestmentRepository;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class InvestmentRepositoryAdapterTest {

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private SpringUserJpaRepository userJpaRepository;

    private InvestmentRepositoryPort adapter;

    private static final String EMAIL = "investor@test.com";

    @BeforeEach
    void setUp() {
        adapter = new InvestmentRepositoryAdapter(investmentRepository, userJpaRepository);

        User user = new User();
        user.setUsername("investor");
        user.setEmail(EMAIL);
        user.setPassword("hash");
        userJpaRepository.save(UserJpaEntity.fromDomain(user));
    }

    @Test
    void saveAll_ThenFindByUserEmail_RoundTripsEveryField() {
        LocalDate purchaseDate = LocalDate.of(2025, 3, 1);
        Investment investment = new Investment(
                null, EMAIL, "PETR4", BigDecimal.valueOf(100.0), BigDecimal.valueOf(30.5), purchaseDate, InvestmentType.STOCKS);

        adapter.saveAll(EMAIL, List.of(investment));
        List<Investment> found = adapter.findByUserEmail(EMAIL);

        assertThat(found).hasSize(1);
        Investment saved = found.get(0);
        assertThat(saved.id()).isNotNull();
        assertThat(saved.userEmail()).isEqualTo(EMAIL);
        assertThat(saved.name()).isEqualTo("PETR4");
        assertThat(saved.quantity()).isEqualByComparingTo("100.0");
        assertThat(saved.purchasePrice()).isEqualByComparingTo("30.5");
        assertThat(saved.purchaseDate()).isEqualTo(purchaseDate);
        assertThat(saved.type()).isEqualTo(InvestmentType.STOCKS);
    }

    @Test
    void saveAll_ForUnknownUserEmail_ThrowsIllegalArgumentException() {
        Investment investment = new Investment(
                null, "ghost@test.com", "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS);

        assertThrows(IllegalArgumentException.class, () -> adapter.saveAll("ghost@test.com", List.of(investment)));
    }

    @Test
    void findByUserEmail_WithNoInvestments_ReturnsEmptyList() {
        assertThat(adapter.findByUserEmail(EMAIL)).isEmpty();
    }

    @Test
    void deleteByUserEmail_RemovesAllOfThatUsersInvestments() {
        Investment investment = new Investment(
                null, EMAIL, "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS);
        adapter.saveAll(EMAIL, List.of(investment));

        adapter.deleteByUserEmail(EMAIL);

        assertThat(adapter.findByUserEmail(EMAIL)).isEmpty();
    }

    @Test
    void findByUserEmail_IsolatedPerUser() {
        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("hash");
        userJpaRepository.save(UserJpaEntity.fromDomain(otherUser));

        adapter.saveAll(EMAIL, List.of(new Investment(
                null, EMAIL, "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS)));
        adapter.saveAll("other@test.com", List.of(new Investment(
                null, "other@test.com", "VALE3", BigDecimal.valueOf(2), BigDecimal.valueOf(2), LocalDate.now(), InvestmentType.STOCKS)));

        assertThat(adapter.findByUserEmail(EMAIL)).hasSize(1);
        assertThat(adapter.findByUserEmail(EMAIL).get(0).name()).isEqualTo("PETR4");
    }
}
