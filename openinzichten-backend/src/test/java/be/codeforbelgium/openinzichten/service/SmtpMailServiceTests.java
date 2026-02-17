package be.codeforbelgium.openinzichten.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

class SmtpMailServiceTests {

    @Test
    void sendPasswordReset_no_mailSender_logs_and_returns() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        SmtpMailService svc = new SmtpMailService(provider, "noreply@example.com");
        Assertions.assertDoesNotThrow(() -> svc.sendPasswordReset("to@example.com", "link"));
    }

    @Test
    void sendPasswordReset_with_mailSender_sends_message() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        SmtpMailService svc = new SmtpMailService(provider, "noreply@example.com");
        svc.sendPasswordReset("to@example.com", "link");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAccountDisabled_no_mailSender_logs_and_returns() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        SmtpMailService svc = new SmtpMailService(provider, "noreply@example.com");
        Assertions.assertDoesNotThrow(() -> svc.sendAccountDisabled("to@example.com", "user", "reason"));
    }

    @Test
    void sendAccountDisabled_with_mailSender_sends_message_and_contains_reason() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        SmtpMailService svc = new SmtpMailService(provider, "noreply@example.com");
        svc.sendAccountDisabled("to@example.com", "user", "the reason");

        // capture the sent message
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAccountReactivated_with_mailSender_sends_message() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        SmtpMailService svc = new SmtpMailService(provider, "noreply@example.com");
        svc.sendAccountReactivated("to@example.com", "user");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
