package com.jf.PetApp.core.domain.enums;

/**
 * Corporate-action type as reported by the market data provider's {@code label}
 * field (e.g. Brapi's dividends endpoint). Kept separate from a free-text label
 * so the client can render a consistent icon/color without string-matching
 * provider text itself.
 */
public enum DividendType {
    DIVIDENDO,
    JCP,
    RENDIMENTO,
    OUTRO;

    /**
     * Maps a provider's raw label to a known type. Never guesses a specific
     * type it isn't confident about — unrecognized labels fall back to
     * {@link #OUTRO} rather than being misclassified.
     */
    public static DividendType fromRawLabel(String rawLabel) {
        if (rawLabel == null) return OUTRO;
        String normalized = rawLabel.trim().toUpperCase();
        return switch (normalized) {
            case "DIVIDENDO", "DIVIDEND" -> DIVIDENDO;
            case "JCP", "JUROS SOBRE CAPITAL PROPRIO", "JUROS SOBRE CAPITAL PRÓPRIO" -> JCP;
            case "RENDIMENTO" -> RENDIMENTO;
            default -> OUTRO;
        };
    }
}
