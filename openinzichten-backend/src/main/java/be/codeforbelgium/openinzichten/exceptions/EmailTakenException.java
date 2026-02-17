package be.codeforbelgium.openinzichten.exceptions;

public class EmailTakenException extends RuntimeException {
    public EmailTakenException(String email) {
        super("Email already in use");
    }
}
