package com.jf.PetApp.application.lab.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.application.gamification.service.TotalXpCalculator;
import com.jf.PetApp.application.lab.dto.SimulatorProgressResult;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.XpEventType;

class GetSimulatorProgressUseCaseImplTest {

    private static final String EMAIL = "learner@test.com";
    private static final Long USER_ID = 42L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private XpEventRepositoryPort xpEventRepositoryPort;

    @Mock
    private TotalXpCalculator totalXpCalculator;

    private GetSimulatorProgressUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetSimulatorProgressUseCaseImpl(userRepository, xpEventRepositoryPort, totalXpCalculator);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void execute_UnknownUser_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute("ghost@test.com"));
    }

    @Test
    void execute_ReturnsCompletedSimulatorIdsAndTotals() {
        when(xpEventRepositoryPort.sourceIdsByUserIdAndEventType(USER_ID, XpEventType.SIMULATOR_COMPLETED))
                .thenReturn(Set.of("compound_interest", "inflation"));
        when(totalXpCalculator.totalXpFor(USER_ID)).thenReturn(100);

        SimulatorProgressResult result = useCase.execute(EMAIL);

        assertEquals(Set.of("compound_interest", "inflation"), result.completedSimulatorIds());
        assertEquals(100, result.totalXp());
    }

    @Test
    void execute_NoCompletions_ReturnsEmptySet() {
        when(xpEventRepositoryPort.sourceIdsByUserIdAndEventType(USER_ID, XpEventType.SIMULATOR_COMPLETED))
                .thenReturn(Set.of());
        when(totalXpCalculator.totalXpFor(USER_ID)).thenReturn(0);

        SimulatorProgressResult result = useCase.execute(EMAIL);

        assertEquals(Set.of(), result.completedSimulatorIds());
    }
}
