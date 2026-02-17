package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.ZipcodeResponse;
import be.codeforbelgium.openinzichten.domain.Zipcode;
import be.codeforbelgium.openinzichten.repository.ZipcodeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZipcodeServiceTests {

    @Test
    void findGemeentenByZipcode_trims_and_dedups() {
        ZipcodeRepository repo = mock(ZipcodeRepository.class);
        ZipcodeService svc = new ZipcodeService(repo);

        Zipcode z1 = new Zipcode(null, "1000", "Brussel", "B", "G", null);
        Zipcode z2 = new Zipcode(null, "1000", "  Brussel  ", "B", "G", null);
        Zipcode z3 = new Zipcode(null, "1000", "", "B", "G", null);

        when(repo.findAllByCode("1000")).thenReturn(List.of(z1, z2, z3));

        List<String> gemeenten = svc.findGemeentenByZipcode("1000");
        assertEquals(1, gemeenten.size());
        assertEquals("Brussel", gemeenten.get(0));
    }

    @Test
    void findAllZipcodes_maps_to_response() {
        ZipcodeRepository repo = mock(ZipcodeRepository.class);
        ZipcodeService svc = new ZipcodeService(repo);

        Zipcode z = new Zipcode(null, "2000", "Antwerpen", "P", "G", null);
        when(repo.findAll()).thenReturn(List.of(z));

        List<ZipcodeResponse> res = svc.findAllZipcodes();
        assertEquals(1, res.size());
        ZipcodeResponse r = res.get(0);
        assertEquals("2000", r.code());
        assertEquals("Antwerpen", r.gemeente());
    }
}
