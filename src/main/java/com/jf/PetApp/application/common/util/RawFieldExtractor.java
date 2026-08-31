package com.jf.PetApp.application.common.util;

/**
 * Null-safe type coercion for values pulled out of a raw, dynamically-shaped
 * {@code Map<String, Object>} — the shape external market-data providers hand back before (or
 * without) being mapped onto a typed DTO. Shared between the use-case layer
 * ({@code GetAssetDetailsUseCaseImpl}/{@code AssetDetailsResponseMapper}, mapping a provider
 * response into {@code AssetDetailsResponseDTO}) and the infrastructure layer
 * ({@code BrapiInvestmentApiClient}, parsing the raw HTTP response), so both sides agree on
 * what "the provider didn't return this field" looks like: {@code null}, never a fabricated
 * zero or empty string.
 */
public final class RawFieldExtractor {

    private RawFieldExtractor() {
    }

    public static Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    public static Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    public static String toStringOrNull(Object value) {
        return value instanceof String s && !s.isBlank() ? s : null;
    }
}
