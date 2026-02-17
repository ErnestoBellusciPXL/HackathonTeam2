package be.codeforbelgium.openinzichten.api.response;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AllConditionsMunicipalityCountTests {

    @Test
    void constructorAndGetters_workAsExpected() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("condA", 10L);

        AllConditionsMunicipalityCount dto = new AllConditionsMunicipalityCount("MyTown", "12345", counts);

        assertEquals("MyTown", dto.getMunicipality());
        assertEquals("12345", dto.getMunicipalityCode());
        assertNotNull(dto.getCountsByCondition());
        assertEquals(1, dto.getCountsByCondition().size());
        assertEquals(10L, dto.getCountsByCondition().get("condA"));
    }

    @Test
    void defaultConstructorAndSetters_workAsExpected() {
        AllConditionsMunicipalityCount dto = new AllConditionsMunicipalityCount();

        dto.setMunicipality("OtherTown");
        dto.setMunicipalityCode("99999");

        Map<String, Long> m = new HashMap<>();
        m.put("condX", 3L);
        dto.setCountsByCondition(m);

        assertEquals("OtherTown", dto.getMunicipality());
        assertEquals("99999", dto.getMunicipalityCode());
        assertEquals(1, dto.getCountsByCondition().size());

        // ensure returned map is the same instance
        dto.getCountsByCondition().put("condY", 5L);
        assertEquals(2, dto.getCountsByCondition().size());
        assertEquals(5L, dto.getCountsByCondition().get("condY"));
    }
}
