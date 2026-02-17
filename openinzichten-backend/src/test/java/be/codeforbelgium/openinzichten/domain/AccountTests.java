package be.codeforbelgium.openinzichten.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTests {

    @Test
    void builder_and_getters() {
        Account a = Account.builder()
                .username("bob")
                .email("bob@example.com")
                .password("secret")
                .zipcode("1000")
                .hasCondition(true)
                .build();

        assertEquals("bob", a.getUsername());
        assertEquals("bob@example.com", a.getEmail());
        assertEquals("secret", a.getPassword());
        assertEquals("1000", a.getZipcode());
        assertTrue(a.isHasCondition());
    }

    @Test
    void setters_and_collections() {
        Account a = new Account();
        a.setUsername("alice");
        a.setEmail("alice@example.com");
        a.setPassword("pw");

        Community c = new Community();
        c.setName("C1");

        Condition cond = new Condition();
        cond.setName("Cond1");

        HashSet<Community> comms = new HashSet<>();
        comms.add(c);
        a.setCommunities(comms);

        HashSet<Condition> conds = new HashSet<>();
        conds.add(cond);
        a.setConditions(conds);

        assertEquals("alice", a.getUsername());
        assertEquals(1, a.getCommunities().size());
        assertEquals(1, a.getConditions().size());
    }
}
