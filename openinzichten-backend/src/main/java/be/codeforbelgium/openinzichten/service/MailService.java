package be.codeforbelgium.openinzichten.service;

public interface MailService {
    void sendPasswordReset(String to, String resetLink);
    void sendAccountDisabled(String to, String username, String reason);
    void sendAccountReactivated(String to, String username);
}
