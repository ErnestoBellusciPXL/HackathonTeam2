package be.codeforbelgium.openinzichten.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpMailServiceExtraTest {

    @Test
    void whenNoMailSender_loggingPathDoesNotThrow() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = (ObjectProvider<JavaMailSender>) Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        var svc = new SmtpMailService(provider, "from@x.com");
        // should not throw
       
        assertThatCode(() -> {
            svc.sendPasswordReset("to@example.com", "http://reset/token");
            svc.sendAccountDisabled("to@example.com", "user", null);
            svc.sendAccountReactivated("to@example.com", "user");
        }).doesNotThrowAnyException();
    }

    @Test
    void whenMailSenderPresent_sendsMessages() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = (ObjectProvider<JavaMailSender>) Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        var svc = new SmtpMailService(provider, "from@x.com");

        // call all three sends
        svc.sendPasswordReset("to@example.com", "http://reset/token");
        svc.sendAccountDisabled("to2@example.com", "alice", "reason");
        svc.sendAccountReactivated("to3@example.com", "bob");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.times(3)).send(captor.capture());
        var sentList = captor.getAllValues();

        SimpleMailMessage sent0 = sentList.get(0);
        assertThat(sent0.getTo()).containsExactly("to@example.com");
        assertThat(sent0.getText()).contains("http://reset/token");

        SimpleMailMessage sent1 = sentList.get(1);
        assertThat(sent1.getTo()).containsExactly("to2@example.com");
        assertThat(sent1.getText()).contains("reason");

        SimpleMailMessage sent2 = sentList.get(2);
        assertThat(sent2.getTo()).containsExactly("to3@example.com");
    }
}
