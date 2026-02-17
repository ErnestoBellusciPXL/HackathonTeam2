package be.codeforbelgium.openinzichten.exceptions;

public class UsernameTooLongException extends RuntimeException {

    public UsernameTooLongException(String username) {
        super("Username is too long: " + username);
    }

    public UsernameTooLongException(String message, Throwable cause) {
        super(message, cause);
    }
}
