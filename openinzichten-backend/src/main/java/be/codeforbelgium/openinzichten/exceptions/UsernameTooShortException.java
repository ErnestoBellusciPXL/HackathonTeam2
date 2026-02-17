package be.codeforbelgium.openinzichten.exceptions;

public class UsernameTooShortException extends RuntimeException {

    public UsernameTooShortException(String username) {
        super("Username is too short: " + username);
    }

    public UsernameTooShortException(String message, Throwable cause) {
        super(message, cause);
    }
}
