package be.codeforbelgium.openinzichten.exceptions;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String idOrUsername) {
        super("Account not found: " + idOrUsername);
    }

}
