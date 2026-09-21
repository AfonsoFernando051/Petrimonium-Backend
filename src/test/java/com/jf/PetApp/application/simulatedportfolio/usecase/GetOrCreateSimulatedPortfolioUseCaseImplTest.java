package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetOrCreateSimulatedPortfolioUseCaseImplTest {

    @Mock
    private SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;

    @Mock
    private UserRepository userRepository;

    private GetOrCreateSimulatedPortfolioUseCaseImpl useCase;

    private static final String EMAIL = "learner@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetOrCreateSimulatedPortfolioUseCaseImpl(
                simulatedPortfolioRepository, userRepository);
    }

    @Test
    void execute_WhenPortfolioAlreadyExists_ReturnsItWithoutCreatingANewOne() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User()));
        SimulatedPortfolio existing = new SimulatedPortfolio(
                1L, EMAIL, "BRL", null, Instant.now(), Instant.now());
        when(simulatedPortfolioRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));

        SimulatedPortfolio result = useCase.execute(EMAIL);

        assertEquals(existing, result);
        verify(simulatedPortfolioRepository, never()).create(any(), any());
    }

    @Test
    void execute_WhenNoPortfolioYet_CreatesAnEmptyOneWithNoStartingBalance() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User()));
        when(simulatedPortfolioRepository.findByUserEmail(EMAIL)).thenReturn(Optional.empty());
        SimulatedPortfolio created = new SimulatedPortfolio(
                1L, EMAIL, "BRL", null, Instant.now(), Instant.now());
        when(simulatedPortfolioRepository.create(EMAIL, "BRL")).thenReturn(created);

        SimulatedPortfolio result = useCase.execute(EMAIL);

        assertEquals(created, result);
        verify(simulatedPortfolioRepository).create(eq(EMAIL), eq("BRL"));
    }

    @Test
    void execute_WhenUserDoesNotExist_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute("missing@test.com"));

        verify(simulatedPortfolioRepository, never()).findByUserEmail(any());
        verify(simulatedPortfolioRepository, never()).create(any(), any());
    }
}
