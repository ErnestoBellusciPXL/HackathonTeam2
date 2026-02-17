package be.codeforbelgium.openinzichten.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CommunityTests {

    @Test
    void name_and_equals() {
        Community c1 = new Community();
        c1.setName("Health");

        Community c2 = new Community();
        c2.setName("Health");

        Community c3 = new Community();
        c3.setName("Other");

        assertEquals("Health", c1.getName());
        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
    }
}
