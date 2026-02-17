package be.codeforbelgium.openinzichten.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PasswordResetTokenTests {

    @Test
    void builder_and_fields() {
        Account a = Account.builder().username("u").email("u@example.com").password("p").build();
        Instant now = Instant.now();

        PasswordResetToken t = PasswordResetToken.builder()
                .token("tok-1")
                .account(a)
                .expiry(now)
                .build();

        assertEquals("tok-1", t.getToken());
        assertSame(a, t.getAccount());
        assertEquals(now, t.getExpiry());
    }

    @Test
    void setters_and_getters() {
        PasswordResetToken t = new PasswordResetToken();
        t.setToken("x");
        t.setExpiry(Instant.EPOCH);

        assertEquals("x", t.getToken());
        assertEquals(Instant.EPOCH, t.getExpiry());
    }
}
