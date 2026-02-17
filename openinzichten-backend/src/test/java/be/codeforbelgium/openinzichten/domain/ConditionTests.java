package be.codeforbelgium.openinzichten.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConditionTests {

    @Test
    void name_and_communities() {
        Condition cond = new Condition();
        cond.setName("Diabetes");

        Community c = new Community();
        c.setName("Diabetes Support");

        HashSet<Community> set = new HashSet<>();
        set.add(c);
        cond.setCommunities(set);

        assertEquals("Diabetes", cond.getName());
        assertNotNull(cond.getCommunities());
        assertEquals(1, cond.getCommunities().size());
    }
}
