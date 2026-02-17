package be.codeforbelgium.openinzichten.exceptions;

public class PasswordResetTokenNotFoundException extends RuntimeException {
    public PasswordResetTokenNotFoundException(String token) {
        super("Password reset token not found: " + token);
    }
}
