package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.MunicipalityCommunitymemberCountResponse;
import be.codeforbelgium.openinzichten.api.response.SingleConditionMunicipalityCount;
import be.codeforbelgium.openinzichten.service.ConditionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeatmapControllerTests {

    private ConditionService conditionService;
    private HeatmapController controller;

    @BeforeEach
    void setUp() {
        conditionService = mock(ConditionService.class);
        controller = new HeatmapController(conditionService);
    }

    @Test
    void getCountsByMunicipality_whenConditionIsNull_returnsCommunityCounts() {
        MunicipalityCommunitymemberCountResponse dto = new MunicipalityCommunitymemberCountResponse("Gent", "44021", 42L);
        when(conditionService.getCommunitymemberCountsByMunicipality()).thenReturn(List.of(dto));

        ResponseEntity<?> resp = controller.getCountsByMunicipality(null);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        Object body = resp.getBody();
        assertNotNull(body);
        assertTrue(body instanceof List);
        List<?> list = (List<?>) body;
        assertEquals(1, list.size());
        assertTrue(list.get(0) instanceof MunicipalityCommunitymemberCountResponse);
        MunicipalityCommunitymemberCountResponse item = (MunicipalityCommunitymemberCountResponse) list.get(0);
        assertEquals("Gent", item.municipality());
        assertEquals("44021", item.municipalityCode());
        assertEquals(42L, item.amountOfCommunitymembers());

        verify(conditionService, times(1)).getCommunitymemberCountsByMunicipality();
    }

    @Test
    void getCountsByMunicipality_whenConditionIsBlank_returnsCommunityCounts() {
        MunicipalityCommunitymemberCountResponse dto = new MunicipalityCommunitymemberCountResponse("Antwerpen", "11002", 7L);
        when(conditionService.getCommunitymemberCountsByMunicipality()).thenReturn(List.of(dto));

        ResponseEntity<?> resp = controller.getCountsByMunicipality("   ");

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        Object body = resp.getBody();
        assertNotNull(body);
        assertTrue(body instanceof List);
        List<?> list = (List<?>) body;
        assertEquals(1, list.size());

        verify(conditionService, times(1)).getCommunitymemberCountsByMunicipality();
    }

    @Test
    void getCountsByMunicipality_whenConditionProvided_callsServiceWithTrimmedValue() {
        SingleConditionMunicipalityCount entry = new SingleConditionMunicipalityCount("Mechelen", "12003", 5L);
        when(conditionService.getCountsByMunicipality("asthma")).thenReturn(List.of(entry));

        ResponseEntity<?> resp = controller.getCountsByMunicipality(" asthma ");

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        Object body = resp.getBody();
        assertNotNull(body);
        assertTrue(body instanceof List);
        List<?> list = (List<?>) body;
        assertEquals(1, list.size());
        assertTrue(list.get(0) instanceof SingleConditionMunicipalityCount);
        SingleConditionMunicipalityCount item = (SingleConditionMunicipalityCount) list.get(0);
        assertEquals("Mechelen", item.getMunicipality());
        assertEquals("12003", item.getMunicipalityCode());
        assertEquals(5L, item.getCount());

        verify(conditionService, times(1)).getCountsByMunicipality("asthma");
    }

}
