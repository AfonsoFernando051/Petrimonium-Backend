package com.jf.PetApp.application.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RawFieldExtractorTest {

    @Test
    void toDouble_IntegerValue_ReturnsDoubleValue() {
        assertEquals(42.0, RawFieldExtractor.toDouble(42));
    }

    @Test
    void toDouble_DoubleValue_ReturnsSameValue() {
        assertEquals(3.14, RawFieldExtractor.toDouble(3.14));
    }

    @Test
    void toDouble_NonNumberValue_ReturnsNull() {
        assertNull(RawFieldExtractor.toDouble("not a number"));
    }

    @Test
    void toDouble_NullValue_ReturnsNull() {
        assertNull(RawFieldExtractor.toDouble(null));
    }

    @Test
    void toLong_IntegerValue_ReturnsLongValue() {
        assertEquals(42L, RawFieldExtractor.toLong(42));
    }

    @Test
    void toLong_DoubleValue_TruncatesToLong() {
        assertEquals(3L, RawFieldExtractor.toLong(3.99));
    }

    @Test
    void toLong_NonNumberValue_ReturnsNull() {
        assertNull(RawFieldExtractor.toLong("not a number"));
    }

    @Test
    void toLong_NullValue_ReturnsNull() {
        assertNull(RawFieldExtractor.toLong(null));
    }

    @Test
    void toStringOrNull_NonBlankString_ReturnsIt() {
        assertEquals("hello", RawFieldExtractor.toStringOrNull("hello"));
    }

    @Test
    void toStringOrNull_BlankString_ReturnsNull() {
        assertNull(RawFieldExtractor.toStringOrNull("   "));
    }

    @Test
    void toStringOrNull_EmptyString_ReturnsNull() {
        assertNull(RawFieldExtractor.toStringOrNull(""));
    }

    @Test
    void toStringOrNull_NonStringValue_ReturnsNull() {
        assertNull(RawFieldExtractor.toStringOrNull(123));
    }

    @Test
    void toStringOrNull_NullValue_ReturnsNull() {
        assertNull(RawFieldExtractor.toStringOrNull(null));
    }
}
