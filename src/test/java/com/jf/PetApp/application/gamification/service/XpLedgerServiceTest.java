package com.jf.PetApp.application.gamification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.core.domain.gamification.XpEventType;

class XpLedgerServiceTest {

    @Mock
    private XpEventRepositoryPort xpEventRepository;

    private XpLedgerService xpLedgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        xpLedgerService = new XpLedgerService(xpEventRepository);
    }

    @Test
    void grantXp_WhenNotAlreadyGranted_SavesAndReturnsTrue() {
        Long userId = 1L;
        when(xpEventRepository.existsByUserIdAndEventTypeAndSourceId(userId, XpEventType.LESSON_COMPLETED, "lesson_1"))
                .thenReturn(false);

        boolean granted = xpLedgerService.grantXp(userId, XpEventType.LESSON_COMPLETED, 20, "lesson_1");

        assertTrue(granted);
        verify(xpEventRepository).save(eq(userId), eq(XpEventType.LESSON_COMPLETED), eq(20), eq("lesson_1"));
    }

    @Test
    void grantXp_WhenAlreadyGranted_IsANoOpAndReturnsFalse() {
        Long userId = 1L;
        when(xpEventRepository.existsByUserIdAndEventTypeAndSourceId(userId, XpEventType.LESSON_COMPLETED, "lesson_1"))
                .thenReturn(true);

        boolean granted = xpLedgerService.grantXp(userId, XpEventType.LESSON_COMPLETED, 20, "lesson_1");

        assertFalse(granted);
        verify(xpEventRepository, never()).save(any(), any(), anyInt(), any());
    }

    @Test
    void totalXpFor_DelegatesToRepositorySum() {
        Long userId = 1L;
        when(xpEventRepository.sumAmountByUserId(userId)).thenReturn(140);

        assertEquals(140, xpLedgerService.totalXpFor(userId));
    }
}
