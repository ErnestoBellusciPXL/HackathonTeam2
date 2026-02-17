package be.codeforbelgium.openinzichten.api.request;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CompleteRegistrationRequestTests {

    @Test
    void allArgsConstructor_and_getters() {
        CompleteRegistrationRequest r = new CompleteRegistrationRequest("1000", Set.of("Diabetes"), true);
        assertEquals("1000", r.getZipcode());
        assertTrue(r.isHasCondition());
        assertTrue(r.getConditions().contains("Diabetes"));
    }

    @Test
    void setters_and_noArgsConstructor() {
        CompleteRegistrationRequest r = new CompleteRegistrationRequest();
        r.setZipcode("2000");
        r.setConditions(Set.of("Asthma"));
        r.setHasCondition(false);

        assertEquals("2000", r.getZipcode());
        assertFalse(r.isHasCondition());
        assertTrue(r.getConditions().contains("Asthma"));
    }

}
