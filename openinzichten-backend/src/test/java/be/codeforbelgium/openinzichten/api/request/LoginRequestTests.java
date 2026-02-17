package be.codeforbelgium.openinzichten.api.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTests {

    @Test
    void allArgsConstructor_and_getters() {
        LoginRequest r = new LoginRequest("bob@example.com", "passwd123", true);
        assertEquals("bob@example.com", r.getEmail());
        assertEquals("passwd123", r.getPassword());
        assertTrue(r.getRememberMe());
    }

    @Test
    void setters_and_noArgsConstructor() {
        LoginRequest r = new LoginRequest();
        r.setEmail("alice@example.com");
        r.setPassword("secret");
        r.setRememberMe(false);

        assertEquals("alice@example.com", r.getEmail());
        assertEquals("secret", r.getPassword());
        assertFalse(r.getRememberMe());
    }

}
