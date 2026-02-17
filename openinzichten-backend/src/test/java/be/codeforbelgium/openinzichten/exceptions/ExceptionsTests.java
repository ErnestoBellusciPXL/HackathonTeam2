package be.codeforbelgium.openinzichten.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionsTests {

    @Test
    void accountNotFound_message() {
        AccountNotFoundException ex = new AccountNotFoundException("id-123");
        assertEquals("Account not found: id-123", ex.getMessage());
    }

    @Test
    void emailTaken_message() {
        EmailTakenException ex = new EmailTakenException("me@example.com");
        assertEquals("Email already in use", ex.getMessage());
    }

    @Test
    void invalidEmail_messages_and_cause() {
        InvalidEmailException ex = new InvalidEmailException("bad@e");
        assertEquals("Invalid email: bad@e", ex.getMessage());

        Throwable cause = new RuntimeException("root");
        InvalidEmailException ex2 = new InvalidEmailException("custom message", cause);
        assertEquals("custom message", ex2.getMessage());
        assertSame(cause, ex2.getCause());
    }

    @Test
    void invalidPassword_messages_and_cause() {
        InvalidCredentialsException ex = new InvalidCredentialsException();
        assertEquals("Combinatie van e-mailadres en wachtwoord is ongeldig.", ex.getMessage());

        InvalidCredentialsException ex2 = new InvalidCredentialsException("bad", new IllegalArgumentException("x"));
        assertEquals("bad", ex2.getMessage());
        assertInstanceOf(IllegalArgumentException.class, ex2.getCause());
    }

    @Test
    void passwordResetToken_exceptions_messages() {
        PasswordResetTokenExpiredException e1 = new PasswordResetTokenExpiredException("tok123");
        assertEquals("Password reset token expired: tok123", e1.getMessage());

        PasswordResetTokenNotFoundException e2 = new PasswordResetTokenNotFoundException("tok999");
        assertEquals("Password reset token not found: tok999", e2.getMessage());
    }

    @Test
    void username_taken_and_length_exceptions() {
        UsernameTakenException ut = new UsernameTakenException("bob");
        assertEquals("Username already in use", ut.getMessage());

        UsernameTooLongException l = new UsernameTooLongException("longname");
        assertEquals("Username is too long: longname", l.getMessage());

        UsernameTooShortException s = new UsernameTooShortException("x");
        assertEquals("Username is too short: x", s.getMessage());

        // test overload with message and cause for UsernameTooLong
        Throwable cause = new RuntimeException("c");
        UsernameTooLongException l2 = new UsernameTooLongException("msg", cause);
        assertEquals("msg", l2.getMessage());
        assertSame(cause, l2.getCause());

        // test overload with message and cause for UsernameTooShort
        Throwable cause2 = new RuntimeException("c2");
        UsernameTooShortException s2 = new UsernameTooShortException("short-msg", cause2);
        assertEquals("short-msg", s2.getMessage());
        assertSame(cause2, s2.getCause());
    }
}
