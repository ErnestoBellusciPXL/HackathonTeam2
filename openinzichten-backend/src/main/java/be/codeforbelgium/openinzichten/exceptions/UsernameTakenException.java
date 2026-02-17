package be.codeforbelgium.openinzichten.exceptions;

public class UsernameTakenException extends RuntimeException {
    public UsernameTakenException(String username) {
        super("Username already in use");
    }
}
