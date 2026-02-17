package be.codeforbelgium.openinzichten.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LoggingMailServiceTests {

    @Test
    void sendPasswordReset_no_exception() {
        LoggingMailService svc = new LoggingMailService();
        Assertions.assertDoesNotThrow(() -> svc.sendPasswordReset("me@example.com", "http://reset/token"));
    }

    @Test
    void sendAccountDisabled_no_exception() {
        LoggingMailService svc = new LoggingMailService();
        Assertions.assertDoesNotThrow(() -> svc.sendAccountDisabled("to@example.com", "user", "reason"));
    }

    @Test
    void sendAccountReactivated_no_exception() {
        LoggingMailService svc = new LoggingMailService();
        Assertions.assertDoesNotThrow(() -> svc.sendAccountReactivated("to@example.com", "user"));
    }
}
