package com.jf.PetApp.application.academy.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.application.academy.port.AcademyCatalogQueryPort;

@ExtendWith(MockitoExtension.class)
class GetAcademyCatalogUseCaseImplTest {

    @Mock
    private AcademyCatalogQueryPort academyCatalogQueryPort;

    private GetAcademyCatalogUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAcademyCatalogUseCaseImpl(academyCatalogQueryPort);
    }

    @Test
    void execute_DelegatesToPortWithGivenLanguageAndReturnsItsResult() {
        AcademyCatalogResult result = new AcademyCatalogResult(List.of(), List.of(), List.of(), List.of());
        when(academyCatalogQueryPort.getCatalog("en")).thenReturn(result);

        AcademyCatalogResult actual = useCase.execute("en");

        assertSame(result, actual);
        verify(academyCatalogQueryPort).getCatalog("en");
    }

    @Test
    void execute_DifferentLanguage_PassesItThroughUnchanged() {
        AcademyCatalogResult result = new AcademyCatalogResult(List.of(), List.of(), List.of(), List.of());
        when(academyCatalogQueryPort.getCatalog("pt")).thenReturn(result);

        useCase.execute("pt");

        verify(academyCatalogQueryPort).getCatalog("pt");
    }

    @Test
    void execute_NullLanguage_PassesNullThroughToPort() {
        AcademyCatalogResult result = new AcademyCatalogResult(List.of(), List.of(), List.of(), List.of());
        when(academyCatalogQueryPort.getCatalog(null)).thenReturn(result);

        AcademyCatalogResult actual = useCase.execute(null);

        assertEquals(result, actual);
        verify(academyCatalogQueryPort).getCatalog(null);
    }
}
