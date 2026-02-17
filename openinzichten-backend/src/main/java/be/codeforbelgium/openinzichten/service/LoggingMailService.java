package be.codeforbelgium.openinzichten.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class LoggingMailService implements MailService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingMailService.class);

    @Override
    public void sendPasswordReset(String to, String resetLink) {
        logger.info("Password reset requested for {}. Reset link: {}", to, resetLink);
    }

    @Override
    public void sendAccountDisabled(String to, String username, String reason) {
        logger.info("Account disabled notification for {} ({}). Reason: {}", username, to, reason);
    }

    @Override
    public void sendAccountReactivated(String to, String username) {
        logger.info("Account reactivated notification for {} ({}).", username, to);
    }
}
