package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ConfigureInvestmentsUseCaseImplTest {

    @Mock
    private InvestmentRepositoryPort investmentRepositoryPort;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConfigureInvestmentsUseCaseImpl configureInvestmentsUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WhenUserExists_ShouldReplaceInvestments() {
        String email = "investor@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User()));

        ConfigureInvestmentCommand asset1 = new ConfigureInvestmentCommand("PETR4", 100.0, 35.5, java.time.LocalDate.now(), InvestmentType.STOCKS);
        ConfigureInvestmentCommand asset2 = new ConfigureInvestmentCommand("BTC", 0.5, 300000.0, java.time.LocalDate.now(), InvestmentType.CRYPTO);

        configureInvestmentsUseCase.execute(email, List.of(asset1, asset2));

        verify(investmentRepositoryPort, times(1)).deleteByUserEmail(email);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.jf.PetApp.core.domain.Investment>> captor = ArgumentCaptor.forClass(List.class);
        verify(investmentRepositoryPort, times(1)).saveAll(eq(email), captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals("PETR4", captor.getValue().get(0).name());
        assertEquals(email, captor.getValue().get(0).userEmail());
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowException() {
        String email = "missing@test.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ConfigureInvestmentCommand asset1 = new ConfigureInvestmentCommand("PETR4", 100.0, 35.5, java.time.LocalDate.now(), InvestmentType.STOCKS);

        assertThrows(ResourceNotFoundException.class, () ->
            configureInvestmentsUseCase.execute(email, List.of(asset1)));

        verify(investmentRepositoryPort, never()).saveAll(any(), any());
        verify(investmentRepositoryPort, never()).deleteByUserEmail(any());
    }
}
