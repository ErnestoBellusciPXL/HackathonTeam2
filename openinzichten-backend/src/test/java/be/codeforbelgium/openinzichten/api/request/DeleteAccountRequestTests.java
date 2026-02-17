package be.codeforbelgium.openinzichten.api.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteAccountRequestTests {

    @Test
    void allArgsConstructor_and_getters() {
        DeleteAccountRequest r = new DeleteAccountRequest("secret", "user-1");
        assertEquals("secret", r.getPassword());
        assertEquals("user-1", r.getUserId());
    }

    @Test
    void setters_and_noArgsConstructor() {
        DeleteAccountRequest r = new DeleteAccountRequest();
        r.setPassword("pw");
        r.setUserId("u2");

        assertEquals("pw", r.getPassword());
        assertEquals("u2", r.getUserId());
    }

}
