package be.codeforbelgium.openinzichten.api.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegisterRequestTests {

    @Test
    void allArgsConstructor_and_getters() {
        RegisterRequest r = new RegisterRequest("bob", "bob@example.com", "password1");
        assertEquals("bob", r.getUsername());
        assertEquals("bob@example.com", r.getEmail());
        assertEquals("password1", r.getPassword());
    }

    @Test
    void setters_and_noArgsConstructor() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("alice");
        r.setEmail("alice@example.com");
        r.setPassword("s3cret!");

        assertEquals("alice", r.getUsername());
        assertEquals("alice@example.com", r.getEmail());
        assertEquals("s3cret!", r.getPassword());
    }

}
