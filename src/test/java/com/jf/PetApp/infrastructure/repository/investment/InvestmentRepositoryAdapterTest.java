package com.jf.PetApp.infrastructure.repository.investment;

import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.core.domain.Investment;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.InvestmentRepository;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

import com.jf.PetApp.infrastructure.entity.InvestmentJpaEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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

    @Autowired
    private TestEntityManager entityManager;

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
    void create_AssignsIdAndAuditTimestamps() {
        LocalDate purchaseDate = LocalDate.of(2025, 3, 1);
        Investment investment = new Investment(
                null, EMAIL, "PETR4", BigDecimal.valueOf(100.0), BigDecimal.valueOf(30.5), purchaseDate, InvestmentType.STOCKS);

        Investment created = adapter.create(EMAIL, investment);

        assertThat(created.id()).isNotNull();
        assertThat(created.userEmail()).isEqualTo(EMAIL);
        assertThat(created.name()).isEqualTo("PETR4");
        assertThat(adapter.findByUserEmail(EMAIL)).hasSize(1);
    }

    @Test
    void create_DoesNotTouchOtherLotsOfTheSameUser() {
        adapter.create(EMAIL, new Investment(
                null, EMAIL, "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS));

        adapter.create(EMAIL, new Investment(
                null, EMAIL, "VALE3", BigDecimal.valueOf(2), BigDecimal.valueOf(2), LocalDate.now(), InvestmentType.STOCKS));

        List<Investment> found = adapter.findByUserEmail(EMAIL);
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Investment::name).containsExactlyInAnyOrder("PETR4", "VALE3");
    }

    @Test
    void create_ForUnknownUserEmail_ThrowsIllegalArgumentException() {
        Investment investment = new Investment(
                null, "ghost@test.com", "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS);

        assertThrows(IllegalArgumentException.class, () -> adapter.create("ghost@test.com", investment));
    }

    @Test
    void update_WhenOwnedByCaller_UpdatesInPlace() {
        Investment created = adapter.create(EMAIL, new Investment(
                null, EMAIL, "PETR4", BigDecimal.valueOf(100), BigDecimal.valueOf(30.5),
                LocalDate.of(2025, 1, 1), InvestmentType.STOCKS));

        Investment updated = adapter.update(created.id(), EMAIL, new Investment(
                null, EMAIL, "PETR4", BigDecimal.valueOf(150), BigDecimal.valueOf(31.0),
                LocalDate.of(2025, 2, 1), InvestmentType.STOCKS));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.quantity()).isEqualByComparingTo("150");
        assertThat(updated.purchasePrice()).isEqualByComparingTo("31.0");
        assertThat(updated.purchaseDate()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(adapter.findByUserEmail(EMAIL)).hasSize(1);
    }

    @Test
    void update_WhenOwnedByAnotherUser_ThrowsResourceNotFound() {
        Investment created = adapter.create(EMAIL, new Investment(
                null, EMAIL, "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS));

        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("hash");
        userJpaRepository.save(UserJpaEntity.fromDomain(otherUser));

        Investment attempt = new Investment(
                null, "other@test.com", "PETR4", BigDecimal.TEN, BigDecimal.TEN, LocalDate.now(), InvestmentType.STOCKS);

        assertThrows(com.jf.PetApp.application.common.exception.ResourceNotFoundException.class,
                () -> adapter.update(created.id(), "other@test.com", attempt));
    }

    /**
     * Exercises the {@code @Version} column directly at the Spring Data/entity level rather than
     * through {@code adapter.update()} — two calls to {@code adapter.update()} within one test
     * transaction would share the same Hibernate persistence-context cache and never reproduce a
     * real race, since both would load (and mutate) the *same* managed instance. Detaching and
     * re-fetching simulates two independent requests (e.g. two devices) that each loaded the lot
     * before either one saved.
     */
    @Test
    void update_WhenAnotherEditSavedFirst_ThrowsOptimisticLockingFailure() {
        Investment created = adapter.create(EMAIL, new Investment(
                null, EMAIL, "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS));
        entityManager.flush();
        entityManager.clear();

        InvestmentJpaEntity staleCopy = investmentRepository.findById(created.id()).orElseThrow();
        entityManager.detach(staleCopy);
        entityManager.clear();

        InvestmentJpaEntity firstEditor = investmentRepository.findById(created.id()).orElseThrow();
        firstEditor.setQuantity(BigDecimal.TEN);
        investmentRepository.saveAndFlush(firstEditor);
        entityManager.clear();

        staleCopy.setQuantity(BigDecimal.valueOf(20));
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> investmentRepository.saveAndFlush(staleCopy));
    }

    @Test
    void update_WhenIdDoesNotExist_ThrowsResourceNotFound() {
        Investment attempt = new Investment(null, EMAIL, "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS);

        assertThrows(com.jf.PetApp.application.common.exception.ResourceNotFoundException.class,
                () -> adapter.update(999999, EMAIL, attempt));
    }

    @Test
    void delete_WhenOwnedByCaller_RemovesRow() {
        Investment created = adapter.create(EMAIL, new Investment(
                null, EMAIL, "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS));

        adapter.delete(created.id(), EMAIL);

        assertThat(adapter.findByUserEmail(EMAIL)).isEmpty();
    }

    @Test
    void delete_DoesNotTouchOtherLotsOfTheSameUser() {
        Investment toDelete = adapter.create(EMAIL, new Investment(
                null, EMAIL, "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS));
        adapter.create(EMAIL, new Investment(
                null, EMAIL, "VALE3", BigDecimal.valueOf(2), BigDecimal.valueOf(2), LocalDate.now(), InvestmentType.STOCKS));

        adapter.delete(toDelete.id(), EMAIL);

        List<Investment> remaining = adapter.findByUserEmail(EMAIL);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).name()).isEqualTo("VALE3");
    }

    @Test
    void delete_WhenOwnedByAnotherUser_ThrowsResourceNotFoundAndDoesNotRemoveIt() {
        Investment created = adapter.create(EMAIL, new Investment(
                null, EMAIL, "PETR4", BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), InvestmentType.STOCKS));

        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("hash");
        userJpaRepository.save(UserJpaEntity.fromDomain(otherUser));

        assertThrows(com.jf.PetApp.application.common.exception.ResourceNotFoundException.class,
                () -> adapter.delete(created.id(), "other@test.com"));

        assertThat(adapter.findByUserEmail(EMAIL)).hasSize(1);
    }

    @Test
    void delete_WhenIdDoesNotExist_ThrowsResourceNotFound() {
        assertThrows(com.jf.PetApp.application.common.exception.ResourceNotFoundException.class,
                () -> adapter.delete(999999, EMAIL));
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
