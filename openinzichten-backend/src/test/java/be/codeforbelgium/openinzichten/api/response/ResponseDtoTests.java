package be.codeforbelgium.openinzichten.api.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseDtoTests {

    @Test
    void authResponse_hasToken() {
        AuthResponse r = new AuthResponse("tok123");
        assertEquals("tok123", r.token());
    }

    @Test
    void accountResponse_fields() {
        AccountResponse a = new AccountResponse("id-1", "bob", "bob@example.com", "1000");
        assertEquals("id-1", a.id());
        assertEquals("bob", a.username());
        assertEquals("bob@example.com", a.email());
        assertEquals("1000", a.zipcode());
    }

    @Test
    void accountCompleteResponse_fields() {
        AccountCompleteResponse r = new AccountCompleteResponse(
                "id-2",
                "alice",
                "alice@example.com",
                "2000",
                true,
                java.util.List.of("Diabetes", "Hypertension"),
                java.util.List.of("Diabetes Support", "Heart & Blood Pressure")
        );
        assertEquals("id-2", r.id());
        assertEquals("alice", r.username());
        assertEquals("alice@example.com", r.email());
        assertEquals("2000", r.zipcode());
        assertTrue(r.hasCondition());
        assertTrue(r.conditions().contains("Diabetes"));
        assertTrue(r.communities().contains("Diabetes Support"));
    }

}
