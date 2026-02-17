package be.codeforbelgium.openinzichten.api.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SingleConditionMunicipalityCountTests {

    @Test
    void constructorAndGetters_workAsExpected() {
        SingleConditionMunicipalityCount dto = new SingleConditionMunicipalityCount("Town", "0001", 12L);

        assertEquals("Town", dto.getMunicipality());
        assertEquals("0001", dto.getMunicipalityCode());
        assertEquals(12L, dto.getCount());
    }

    @Test
    void defaultConstructorAndSetters_workAsExpected() {
        SingleConditionMunicipalityCount dto = new SingleConditionMunicipalityCount();

        dto.setMunicipality("Other");
        dto.setMunicipalityCode("9999");
        dto.setCount(7L);

        assertEquals("Other", dto.getMunicipality());
        assertEquals("9999", dto.getMunicipalityCode());
        assertEquals(7L, dto.getCount());
    }
}
