package com.jf.PetApp.application.gamification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jf.PetApp.application.gamification.port.AchievementRepositoryPort;
import com.jf.PetApp.application.gamification.port.MissionRepositoryPort;
import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;

class TotalXpCalculatorTest {

    private static final Long USER_ID = 7L;

    @Mock
    private XpEventRepositoryPort xpEventRepositoryPort;

    @Mock
    private AchievementRepositoryPort achievementRepository;

    @Mock
    private MissionRepositoryPort missionRepository;

    private TotalXpCalculator calculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        XpLedgerService xpLedgerService = new XpLedgerService(xpEventRepositoryPort);
        calculator = new TotalXpCalculator(xpLedgerService, achievementRepository, missionRepository);
    }

    @Test
    void totalXpFor_SumsLedgerAchievementAndMissionXp() {
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(200);
        when(achievementRepository.totalXpFor(USER_ID)).thenReturn(50);
        when(missionRepository.totalXpFor(USER_ID)).thenReturn(90);

        assertEquals(340, calculator.totalXpFor(USER_ID));
    }

    @Test
    void totalXpFor_WithNoXpFromAnySource_IsZero() {
        assertEquals(0, calculator.totalXpFor(USER_ID));
    }
}
