package be.codeforbelgium.openinzichten.exceptions;

public class InvalidEmailException extends RuntimeException {

    public InvalidEmailException(String email) {
        super("Invalid email: " + email);
    }

    public InvalidEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
