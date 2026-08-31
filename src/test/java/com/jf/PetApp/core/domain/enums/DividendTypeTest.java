package com.jf.PetApp.core.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DividendTypeTest {

    @Test
    void fromRawLabel_Null_ReturnsOutro() {
        assertEquals(DividendType.OUTRO, DividendType.fromRawLabel(null));
    }

    @Test
    void fromRawLabel_Dividendo_ReturnsDividendo() {
        assertEquals(DividendType.DIVIDENDO, DividendType.fromRawLabel("DIVIDENDO"));
    }

    @Test
    void fromRawLabel_Dividend_ReturnsDividendo() {
        assertEquals(DividendType.DIVIDENDO, DividendType.fromRawLabel("Dividend"));
    }

    @Test
    void fromRawLabel_Jcp_ReturnsJcp() {
        assertEquals(DividendType.JCP, DividendType.fromRawLabel("jcp"));
    }

    @Test
    void fromRawLabel_JurosSobreCapitalProprioWithoutAccent_ReturnsJcp() {
        assertEquals(DividendType.JCP, DividendType.fromRawLabel("Juros Sobre Capital Proprio"));
    }

    @Test
    void fromRawLabel_JurosSobreCapitalProprioWithAccent_ReturnsJcp() {
        assertEquals(DividendType.JCP, DividendType.fromRawLabel("Juros Sobre Capital Próprio"));
    }

    @Test
    void fromRawLabel_Rendimento_ReturnsRendimento() {
        assertEquals(DividendType.RENDIMENTO, DividendType.fromRawLabel("rendimento"));
    }

    @Test
    void fromRawLabel_UnrecognizedLabel_ReturnsOutro() {
        assertEquals(DividendType.OUTRO, DividendType.fromRawLabel("something else entirely"));
    }

    @Test
    void fromRawLabel_LabelWithSurroundingWhitespace_IsTrimmedBeforeMatching() {
        assertEquals(DividendType.DIVIDENDO, DividendType.fromRawLabel("  dividendo  "));
    }

    @Test
    void values_ContainsAllFourTypes() {
        assertEquals(4, DividendType.values().length);
    }
}
