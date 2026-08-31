package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.translation.port.TranslationClient;
import com.jf.PetApp.application.translation.service.TranslationCacheService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MentorPromptSuggestionsServiceTest {

    private final TranslationClient translationClient = mock(TranslationClient.class);
    private final MentorPromptSuggestionsService service = new MentorPromptSuggestionsService(
            new TranslationCacheService(translationClient));

    @Test
    void returnsAUniqueRandomSampleWithTheRequestedSize() {
        var suggestions = service.getRandomSuggestions("pt", 5);

        assertThat(suggestions).hasSize(5).doesNotHaveDuplicates();
    }

    @Test
    void capsTheSampleAtTheAvailableCatalogSize() {
        var suggestions = service.getRandomSuggestions("pt", 100);

        assertThat(suggestions).hasSize(30).doesNotHaveDuplicates();
    }

    @Test
    void translatesTheSampleToTheRequestedAppLanguage() {
        when(translationClient.translate(anyString(), eq("pt"), eq("en")))
                .thenAnswer(invocation -> Optional.of("EN: " + invocation.getArgument(0)));

        var suggestions = service.getRandomSuggestions("en", 5);

        assertThat(suggestions).hasSize(5).allMatch(prompt -> prompt.startsWith("EN: "));
    }
}
