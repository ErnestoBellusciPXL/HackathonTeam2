package be.codeforbelgium.openinzichten.api.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PasswordResetConfirmRequestTests {

    @Test
    void allArgsConstructor_and_getters() {
        PasswordResetConfirmRequest r = new PasswordResetConfirmRequest("tok-123", "newpass");
        assertEquals("tok-123", r.getToken());
        assertEquals("newpass", r.getNewPassword());
    }

    @Test
    void setters_and_noArgsConstructor() {
        PasswordResetConfirmRequest r = new PasswordResetConfirmRequest();
        r.setToken("t2");
        r.setNewPassword("np");

        assertEquals("t2", r.getToken());
        assertEquals("np", r.getNewPassword());
    }

}
