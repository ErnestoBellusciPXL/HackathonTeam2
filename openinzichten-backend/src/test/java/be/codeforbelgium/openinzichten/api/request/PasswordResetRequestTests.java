package be.codeforbelgium.openinzichten.api.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PasswordResetRequestTests {

    @Test
    void constructor_and_getter_setter() {
        PasswordResetRequest r = new PasswordResetRequest("me@example.com");
        assertEquals("me@example.com", r.getEmail());

        r.setEmail("you@example.com");
        assertEquals("you@example.com", r.getEmail());
    }

    @Test
    void noArgsConstructor_isAvailable() {
        PasswordResetRequest r = new PasswordResetRequest();
        // default email should be null
        assertNull(r.getEmail());
        r.setEmail("now@filled.com");
        assertEquals("now@filled.com", r.getEmail());
    }

}
