package com.jf.PetApp.application.settings;

import java.util.Locale;
import java.util.Set;

/**
 * Preferências que pertencem à conta, não a um app: idioma da interface e país.
 *
 * <p>Os idiomas de interface são deliberadamente uma lista à parte de
 * {@code TranslationCacheService.SUPPORTED_LANGUAGES}. Aquela constante diz
 * para que línguas o serviço sabe <em>traduzir conteúdo</em>; esta diz em que
 * línguas a interface é oferecida. As duas coincidiram por acaso até
 * pt-PT existir: a interface distingue português europeu do brasileiro, mas
 * traduzir conteúdo para pt-PT continua a ser traduzir para pt. Partilhar uma
 * constante fazia o utilizador que escolhia "Português (Portugal)" receber 400
 * ao guardar — e o app engole essa falha, por isso a preferência sumia sem
 * erro visível.
 */
public final class AccountPreferences {

    private AccountPreferences() {}

    /** Idiomas em que a interface dos apps é oferecida. */
    public static final Set<String> INTERFACE_LANGUAGES = Set.of("pt", "pt_PT", "en", "es");

    /** Países onde o ecossistema opera nesta fase. ISO 3166-1 alpha-2. */
    public static final Set<String> COUNTRIES = Set.of("BR", "PT");

    /**
     * Aceita as duas grafias que os apps enviam — Wallet e Academy mandam
     * {@code pt_PT}, o Health mandaria {@code pt-PT} — e devolve a forma
     * canónica {@code idioma_REGIÃO}.
     */
    public static String normalizeLanguage(String raw) {
        if (raw == null) return null;
        String value = raw.trim().replace('-', '_');
        int separator = value.indexOf('_');
        if (separator < 0) return value.toLowerCase(Locale.ROOT);
        return value.substring(0, separator).toLowerCase(Locale.ROOT)
                + "_"
                + value.substring(separator + 1).toUpperCase(Locale.ROOT);
    }

    /**
     * {@code Set.of(...)} lança NPE em {@code contains(null)}, por isso a
     * verificação passa por aqui: entrada nula é entrada não suportada, e o
     * chamador devolve 400, não 500.
     */
    public static boolean isSupportedInterfaceLanguage(String normalized) {
        return normalized != null && INTERFACE_LANGUAGES.contains(normalized);
    }

    /** Ver {@link #isSupportedInterfaceLanguage(String)}. */
    public static boolean isSupportedCountry(String normalized) {
        return normalized != null && COUNTRIES.contains(normalized);
    }

    /** Devolve o código de país em maiúsculas, ou {@code null} se vier nulo. */
    public static String normalizeCountry(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }
}
