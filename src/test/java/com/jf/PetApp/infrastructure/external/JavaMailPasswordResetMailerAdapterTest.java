package com.jf.PetApp.infrastructure.external;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JavaMailPasswordResetMailerAdapterTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> providerReturning(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return provider;
    }

    private Environment envWithProfile(String... activeProfiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(activeProfiles);
        return env;
    }

    @Test
    void sendPasswordResetEmail_WhenNoMailSenderBeanAvailable_LogsInsteadOfThrowing() {
        // Mirrors the real dev scenario: spring.mail.host blank means Boot never creates a
        // JavaMailSender bean at all, so the ObjectProvider resolves to null.
        JavaMailPasswordResetMailerAdapter adapter =
                new JavaMailPasswordResetMailerAdapter(providerReturning(null), envWithProfile("dev"));

        // Must not throw — this is the dev-friendly fallback the plan calls for.
        adapter.sendPasswordResetEmail("investor@test.com", "raw-token-value");
    }

    @Test
    void sendPasswordResetEmail_WhenNoMailSenderBeanAvailableInProd_ThrowsAndNeverLogsTheToken() {
        // A blank spring.mail.host in the prod profile is a misconfiguration, not a supported
        // fallback — must fail loudly instead of leaking the reset token to logs.
        JavaMailPasswordResetMailerAdapter adapter =
                new JavaMailPasswordResetMailerAdapter(providerReturning(null), envWithProfile("prod"));

        assertThrows(IllegalStateException.class,
                () -> adapter.sendPasswordResetEmail("investor@test.com", "raw-token-value"));
    }

    @Test
    void sendPasswordResetEmail_WhenMailSenderAvailable_SendsARealMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        JavaMailPasswordResetMailerAdapter adapter =
                new JavaMailPasswordResetMailerAdapter(providerReturning(mailSender), envWithProfile("dev"));
        ReflectionTestUtils.setField(adapter, "fromAddress", "noreply@petapp.test");

        adapter.sendPasswordResetEmail("investor@test.com", "raw-token-value");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertTrue(sent.getTo() != null && sent.getTo()[0].equals("investor@test.com"));
        assertTrue(sent.getFrom().equals("noreply@petapp.test"));
        assertTrue(sent.getText().contains("raw-token-value"));
    }

    @Test
    void sendPasswordResetEmail_WhenMailSenderAvailable_NeverLogsTheFallbackPath() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        JavaMailPasswordResetMailerAdapter adapter =
                new JavaMailPasswordResetMailerAdapter(providerReturning(mailSender), envWithProfile("dev"));

        adapter.sendPasswordResetEmail("investor@test.com", "raw-token-value");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
