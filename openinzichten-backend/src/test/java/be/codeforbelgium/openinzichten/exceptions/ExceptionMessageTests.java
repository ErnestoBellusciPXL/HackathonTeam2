package be.codeforbelgium.openinzichten.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionMessageTests {

    @Test
    void accountNotFound_message() {
        AccountNotFoundException ex = new AccountNotFoundException("nope");
        assertTrue(ex.getMessage().contains("nope"));
    }

    @Test
    void emailTaken_message() {
        EmailTakenException ex = new EmailTakenException("e@e.com");
        assertTrue(ex.getMessage().contains("Email"));
    }

    @Test
    void invalidEmail_message_and_cause() {
        InvalidEmailException ex = new InvalidEmailException("bad", new RuntimeException("cause"));
        assertEquals("bad", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    void invalidPassword_messages() {
        InvalidCredentialsException ex1 = new InvalidCredentialsException();
        InvalidCredentialsException ex2 = new InvalidCredentialsException("m");
        assertTrue(!ex1.getMessage().isEmpty());
        assertEquals("m", ex2.getMessage());
    }

    @Test
    void usernameTaken_message() {
        UsernameTakenException ex = new UsernameTakenException("u");
        assertTrue(ex.getMessage().contains("Username"));
    }

    @Test
    void usernameLength_messages() {
        UsernameTooLongException l = new UsernameTooLongException("x");
        UsernameTooShortException s = new UsernameTooShortException("y");
        assertTrue(l.getMessage().contains("too long") || !l.getMessage().isEmpty());
        assertTrue(s.getMessage().contains("too short") || !s.getMessage().isEmpty());
    }

}
