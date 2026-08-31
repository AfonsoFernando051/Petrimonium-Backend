package com.jf.PetApp.application.mentor.safety;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A deterministic, server-side check on the Mentor's outgoing reply — the last line of defense
 * behind {@code MentorSystemPromptBuilder}'s safety rules.
 *
 * The system prompt asks the model never to recommend a specific buy/sell, promise a return, or
 * claim to be a licensed adviser, but a prompt instruction alone has no enforcement: a prompt
 * injection or an ordinary model deviation could still slip one of those past it, and today
 * nothing downstream would catch it before the reply reaches the user and gets persisted. This
 * class is that catch — pure, deterministic, no framework dependency, so it's unit-testable in
 * isolation like {@code MentorSystemPromptBuilder}.
 *
 * Deliberately narrow: it flags only the clearest, most legally-relevant violations (a direct
 * buy/sell directive naming what reads as a specific ticker, a guaranteed-return claim, or an
 * explicit licensed-adviser claim) rather than anything that merely mentions an asset — a
 * broader filter would false-positive on ordinary educational language ("many investors buy and
 * hold ETFs for the long term" is fine; "Buy PETR4 now" is not).
 */
public final class MentorSafetyGuard {

    private MentorSafetyGuard() {
    }

    // An imperative buy/sell verb immediately followed by an all-caps token shaped like a
    // ticker (3-6 letters, optionally ending in 1-2 digits — matches both US-style symbols and
    // B3-style symbols like "PETR4"/"ITUB4"). Requires direct adjacency, so it does not fire on
    // prose that merely discusses buying/selling in general. Case-insensitivity is scoped to
    // just the verb group — the ticker group must stay genuinely uppercase, or a global (?i)
    // would let `[A-Z]` match lowercase too (e.g. "buy and hold" matching "and" as a "ticker").
    private static final Pattern DIRECTIVE_WITH_TICKER = Pattern.compile(
            "\\b(?i:buy|sell|compre|compra|venda|vende)\\b\\s+([A-Z]{3,6}\\d{0,2})\\b");

    // "Guaranteed" paired with a return/profit word, in either order, in either language.
    private static final Pattern GUARANTEED_RETURN = Pattern.compile(
            "(?i)\\b(guaranteed|garantid[oa]s?)\\b[^.\\n]{0,40}\\b(return|profit|retorno|lucro|rentabilidade)\\b"
                    + "|\\b(return|profit|retorno|lucro|rentabilidade)\\b[^.\\n]{0,40}\\b(guaranteed|garantid[oa]s?)\\b");

    // An explicit "I am a licensed/certified financial adviser" style claim.
    private static final Pattern LICENSED_ADVISER_CLAIM = Pattern.compile(
            "(?i)\\b(licensed|certified|registered)\\b[^.\\n]{0,30}\\b(financial\\s+)?advis(?:o|e)r\\b"
                    + "|\\bconsultor(?:a)?\\s+financeiro(?:a)?\\b[^.\\n]{0,30}\\b(licenciad|certificad|registrad)[oa]\\b");

    /** Whether {@code reply} contains a clear, deterministic safety-rule violation. */
    public static boolean violatesSafetyRules(String reply) {
        if (reply == null || reply.isBlank()) {
            return false;
        }
        return DIRECTIVE_WITH_TICKER.matcher(reply).find()
                || GUARANTEED_RETURN.matcher(reply).find()
                || LICENSED_ADVISER_CLAIM.matcher(reply).find();
    }

    /**
     * The reply substituted in place of a flagged one — an honest, in-character redirect back
     * toward education rather than a scary error, in the same two languages the product already
     * supports (see {@code MentorSystemPromptBuilder}).
     */
    public static String safeRedirectReply(String language) {
        boolean portuguese = language != null && language.toLowerCase(Locale.ROOT).startsWith("pt");
        return portuguese
                ? "Prefiro não apontar uma compra ou venda específica — isso foge do meu papel aqui. "
                        + "Quer que eu explique o raciocínio por trás disso, usando sua própria carteira como exemplo?"
                : "I'd rather not point at a specific buy or sell — that's outside what I'm here for. "
                        + "Want me to walk through the reasoning instead, using your own portfolio as the example?";
    }
}
