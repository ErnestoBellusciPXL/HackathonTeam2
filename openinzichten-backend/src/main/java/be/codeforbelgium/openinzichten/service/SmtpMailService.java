package be.codeforbelgium.openinzichten.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("!dev")
public class SmtpMailService implements MailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpMailService.class);
    private final JavaMailSender mailSender;
    private final String mailFromAdress;

    public SmtpMailService(ObjectProvider<JavaMailSender> mailSenderProvider, @Value("app.mail.from") String mailFrom) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailFromAdress = mailFrom;
    }

    @Override
    public void sendPasswordReset(String to, String resetLink) {
        if (mailSender == null) {
            // No SMTP configured. Fall back to logging the reset link so the feature
            // still works during development and testing without a real SMTP server.
            logger.warn("No JavaMailSender configured - logging password reset link for {}: {}", to, resetLink);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(this.mailFromAdress);
            msg.setTo(to);
            msg.setSubject("Wachtwoord herstellen");
            StringBuilder sb = new StringBuilder();
            sb.append("Je hebt een verzoek gedaan om je wachtwoord te herstellen. Gebruik de volgende link om je wachtwoord te resetten:\n\n")
              .append(resetLink)
              .append("\n\nAls je dit niet hebt gevraagd, negeer deze e-mail.");
            msg.setText(sb.toString());
            mailSender.send(msg);
        } catch (Exception ex) {
            // Log the full exception and rethrow with contextual information
            logger.error("Failed to send password reset email to {}", to, ex);
        }
    }

    @Override
    public void sendAccountDisabled(String to, String username, String reason) {
        if (mailSender == null) {
            logger.warn("No JavaMailSender configured - logging account disabled for {}: {}", to, reason);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(this.mailFromAdress);
            msg.setTo(to);
            msg.setSubject("Je account is gedeactiveerd");
            StringBuilder sb = new StringBuilder();
            sb.append("Beste ").append(username).append(",\n\n")
              .append("Je account op OpenInzichten is gedeactiveerd door een beheerder.\n\n")
              .append("Reden:\n")
              .append((reason == null || reason.isBlank()) ? "(geen reden opgegeven)" : reason)
              .append("\n\n")
              .append("Als je denkt dat dit onterecht is of vragen hebt, contacteer ons via contact@openinzicht.be.\n\n")
              .append("Met vriendelijke groet,\nHet OpenInzichten team");
            msg.setText(sb.toString());
            mailSender.send(msg);
        } catch (Exception ex) {
            logger.error("Failed to send account disabled email to {}", to, ex);
        }
    }

    @Override
    public void sendAccountReactivated(String to, String username) {
        if (mailSender == null) {
            logger.info("No JavaMailSender configured - logging account reactivated for {}", to);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(this.mailFromAdress);
            msg.setTo(to);
            msg.setSubject("Je account is opnieuw geactiveerd");
            StringBuilder sb = new StringBuilder();
            sb.append("Beste ").append(username).append(",\n\n")
              .append("Je account op OpenInzichten is opnieuw geactiveerd door een beheerder. Je kunt nu weer inloggen en de dienst gebruiken.\n\n")
              .append("Met vriendelijke groet,\nHet OpenInzichten team");
            String text = sb.toString();
            msg.setText(text);
            mailSender.send(msg);
        } catch (Exception ex) {
            logger.error("Failed to send account reactivated email to {}", to, ex);
        }
    }
}
