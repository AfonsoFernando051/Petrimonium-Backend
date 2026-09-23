package com.jf.PetApp.application.simulatedportfolio;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Why a simulated order was refused, worded for the learner who typed it.
 *
 * <p>These are product copy, not developer strings. The mobile client shows a rejected request's
 * {@code detail} verbatim (see {@code friendlyErrorCopy} in
 * {@code petrimonium_shared_features}) — nothing downstream translates it — so a message written
 * only in English surfaces in English inside an otherwise Portuguese app. Same reasoning, and the
 * same per-language switch, as the Mentor's canned replies
 * ({@code GetMentorReplyUseCaseImpl.fallbackReply}, {@code MentorSafetyGuard.safeRedirectReply}).
 *
 * <p>Portuguese is the default for any language this doesn't know, matching the Academy's own
 * {@code Translator.defaultLanguage}. {@code pt_PT} resolves here through the same
 * {@code startsWith("pt")} test the Mentor uses — the wording below is shared by both variants.
 */
public final class SimulatedOrderMessages {

    private SimulatedOrderMessages() {
    }

    public static String quantityMustBePositive(String language) {
        return switch (normalize(language)) {
            case "en" -> "Enter a quantity greater than zero.";
            case "es" -> "Introduce una cantidad mayor que cero.";
            default -> "Informe uma quantidade maior que zero.";
        };
    }

    public static String tickerMustNotBeBlank(String language) {
        return switch (normalize(language)) {
            case "en" -> "Choose an asset before confirming.";
            case "es" -> "Elige un activo antes de confirmar.";
            default -> "Escolha um ativo antes de confirmar.";
        };
    }

    public static String tradeDateMustNotBeInTheFuture(String language) {
        return switch (normalize(language)) {
            case "en" -> "The trade date cannot be in the future.";
            case "es" -> "La fecha de la operación no puede estar en el futuro.";
            default -> "A data da operação não pode estar no futuro.";
        };
    }

    public static String unknownTicker(String language, String ticker) {
        return switch (normalize(language)) {
            case "en" -> "We could not find the asset " + ticker + ".";
            case "es" -> "No encontramos el activo " + ticker + ".";
            default -> "Não encontramos o ativo " + ticker + ".";
        };
    }

    public static String noReferencePrice(String language, String ticker) {
        return switch (normalize(language)) {
            case "en" -> "No price is available for " + ticker + " right now. Try again in a moment.";
            case "es" -> "Ahora mismo no hay cotización para " + ticker + ". Inténtalo de nuevo en un momento.";
            default -> "Não há cotação disponível para " + ticker + " agora. Tente de novo em instantes.";
        };
    }

    public static String noHistoricalPrice(String language, String ticker, Object date) {
        return switch (normalize(language)) {
            case "en" -> "No closing price for " + ticker + " on " + date + ". Pick another date.";
            case "es" -> "No hay precio de cierre de " + ticker + " el " + date + ". Elige otra fecha.";
            default -> "Não há preço de fechamento de " + ticker + " em " + date + ". Escolha outra data.";
        };
    }

    public static String sellBeforeFirstBuy(String language, String ticker, Object sellDate, Object firstBuyDate) {
        return switch (normalize(language)) {
            case "en" -> "You only bought " + ticker + " on " + firstBuyDate
                    + ", so you cannot sell it on " + sellDate + ".";
            case "es" -> "Compraste " + ticker + " el " + firstBuyDate
                    + ", así que no puedes venderlo el " + sellDate + ".";
            default -> "Você só comprou " + ticker + " em " + firstBuyDate
                    + ", então não dá para vender em " + sellDate + ".";
        };
    }

    public static String noPositionHeld(String language, String ticker) {
        return switch (normalize(language)) {
            case "en" -> "You do not hold " + ticker + " in your simulated portfolio.";
            case "es" -> "No tienes " + ticker + " en tu cartera simulada.";
            default -> "Você não tem " + ticker + " na sua carteira simulada.";
        };
    }

    public static String insufficientQuantity(String language, String ticker, BigDecimal held, BigDecimal wanted) {
        String h = plain(held);
        String w = plain(wanted);
        return switch (normalize(language)) {
            case "en" -> "You hold " + h + " of " + ticker + " in your simulated portfolio — you cannot sell " + w + ".";
            case "es" -> "Tienes " + h + " de " + ticker + " en tu cartera simulada — no puedes vender " + w + ".";
            default -> "Você tem " + h + " de " + ticker + " na carteira simulada — não dá para vender " + w + ".";
        };
    }

    public static String resetRequiresConfirmation(String language) {
        return switch (normalize(language)) {
            case "en" -> "Confirm that you want to erase your simulated portfolio.";
            case "es" -> "Confirma que quieres borrar tu cartera simulada.";
            default -> "Confirme que você quer apagar sua carteira simulada.";
        };
    }

    /**
     * A quantity as the learner would write it: {@code BigDecimal}'s own {@code toString} keeps
     * the scale it was parsed with, so ten whole shares read back as "10.0".
     */
    private static String plain(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }

    private static String normalize(String language) {
        if (language == null) {
            return "pt";
        }
        String lower = language.toLowerCase(Locale.ROOT);
        if (lower.startsWith("en")) {
            return "en";
        }
        if (lower.startsWith("es")) {
            return "es";
        }
        return "pt";
    }
}
