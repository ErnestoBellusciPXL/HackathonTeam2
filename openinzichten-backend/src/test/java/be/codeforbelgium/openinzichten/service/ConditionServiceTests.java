package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.ConditionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConditionServiceTests {

    @Test
    void findConditionByName_and_search() {
        ConditionRepository repo = mock(ConditionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MunicipalityGeoJsonService geo = mock(MunicipalityGeoJsonService.class);
        ConditionService svc = new ConditionService(repo, accRepo, geo);

        Condition c = new Condition();
        c.setName("Diabetes");
        when(repo.findByName("Diabetes")).thenReturn(Optional.of(c));

        Optional<Condition> got = svc.findConditionByName("Diabetes");
        assertTrue(got.isPresent());

        when(repo.findByNameContainingIgnoreCase("dia")).thenReturn(List.of(c));
        assertEquals(1, svc.searchByName("dia").size());
    }

    @Test
    void findAll_with_and_without_limit() {
        ConditionRepository repo = mock(ConditionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MunicipalityGeoJsonService geo = mock(MunicipalityGeoJsonService.class);
        ConditionService svc = new ConditionService(repo, accRepo, geo);

        Condition c1 = new Condition();
        c1.setName("A");
        Condition c2 = new Condition();
        c2.setName("B");

        when(repo.findAll(PageRequest.of(0, 1))).thenReturn(new PageImpl<>(List.of(c1)));
        when(repo.findAll()).thenReturn(List.of(c1, c2));

        assertEquals(1, svc.findAll(1).size());
        assertEquals(2, svc.findAll(0).size());
    }

    @Test
    void getConditionsForUser_returnsAccountConditions() {
        ConditionRepository repo = mock(ConditionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MunicipalityGeoJsonService geo = mock(MunicipalityGeoJsonService.class);
        ConditionService svc = new ConditionService(repo, accRepo, geo);

        be.codeforbelgium.openinzichten.domain.Account a = new be.codeforbelgium.openinzichten.domain.Account();
        be.codeforbelgium.openinzichten.domain.Condition c1 = new be.codeforbelgium.openinzichten.domain.Condition();
        c1.setName("X");
        a.setConditions(java.util.Set.of(c1));

        when(accRepo.findWithConditionsByUsername("joe")).thenReturn(java.util.Optional.of(a));

        java.util.Set<be.codeforbelgium.openinzichten.domain.Condition> got = svc.getConditionsForUser("joe");
        assertNotNull(got);
        assertEquals(1, got.size());
        assertEquals("X", got.iterator().next().getName());
    }

    @Test
    void getConditionsForUser_returnsEmptySetIfNull() {
        ConditionRepository repo = mock(ConditionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MunicipalityGeoJsonService geo = mock(MunicipalityGeoJsonService.class);
        ConditionService svc = new ConditionService(repo, accRepo, geo);

        be.codeforbelgium.openinzichten.domain.Account a = new be.codeforbelgium.openinzichten.domain.Account();
        a.setConditions(null);

        when(accRepo.findWithConditionsByUsername("jane")).thenReturn(java.util.Optional.of(a));

        java.util.Set<be.codeforbelgium.openinzichten.domain.Condition> got = svc.getConditionsForUser("jane");
        assertNotNull(got);
        assertTrue(got.isEmpty());
    }

    @Test
    void getCountsByMunicipality_handlesFallbackAndNullValues() {
        ConditionRepository repo = mock(ConditionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MunicipalityGeoJsonService geo = mock(MunicipalityGeoJsonService.class);
        ConditionService svc = new ConditionService(repo, accRepo, geo);

        List<Object[]> rows = List.of(
                new Object[]{"123", "MyTown", 5L},
                new Object[]{null, "OtherTown", null}
        );

        when(accRepo.countAccountsBySpecificConditionGroupedByMunicipality("cond"))
                .thenReturn(rows);
        when(geo.findCodeByMunicipalityName("OtherTown")).thenReturn("999");

        var res = svc.getCountsByMunicipality("cond");

        assertEquals(2, res.size());
        var first = res.stream().filter(r -> "MyTown".equals(r.getMunicipality())).findFirst().orElseThrow();
        assertEquals("123", first.getMunicipalityCode());
        assertEquals(5L, first.getCount());

        var second = res.stream().filter(r -> "OtherTown".equals(r.getMunicipality())).findFirst().orElseThrow();
        assertEquals("999", second.getMunicipalityCode());
        assertEquals(0L, second.getCount());
    }

    @Test
    void getCountsByMunicipalityAllConditions_groupsConditionsAndFallsBackCode() {
        ConditionRepository repo = mock(ConditionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MunicipalityGeoJsonService geo = mock(MunicipalityGeoJsonService.class);
        ConditionService svc = new ConditionService(repo, accRepo, geo);

        // two rows for same municipality but different conditions, and one row for a different municipality
        List<Object[]> rows = List.of(
                new Object[]{"200", "Ville", "A", 2L},
                new Object[]{"200", "Ville", "B", 3L},
                new Object[]{null, "TownX", "A", 1L}
        );

        when(accRepo.countAccountsByAllConditionsGroupedByMunicipality()).thenReturn(rows);
        when(geo.findCodeByMunicipalityName("TownX")).thenReturn("777");

        var res = svc.getCountsByMunicipalityAllConditions();

        // Expect 2 municipalities
        assertEquals(2, res.size());

        var ville = res.stream().filter(r -> "Ville".equals(r.getMunicipality())).findFirst().orElseThrow();
        assertEquals("200", ville.getMunicipalityCode());
        assertEquals(2L, ville.getCountsByCondition().get("A"));
        assertEquals(3L, ville.getCountsByCondition().get("B"));

        var townx = res.stream().filter(r -> "TownX".equals(r.getMunicipality())).findFirst().orElseThrow();
        assertEquals("777", townx.getMunicipalityCode());
        assertEquals(1L, townx.getCountsByCondition().get("A"));
    }

    @Test
    void getCommunitymemberCountsByMunicipality_handlesNullsAndFallbackCode() {
        ConditionRepository repo = mock(ConditionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MunicipalityGeoJsonService geo = mock(MunicipalityGeoJsonService.class);
        ConditionService svc = new ConditionService(repo, accRepo, geo);

        List<Object[]> rows = List.of(
                new Object[]{"10", "City", 7L},
                new Object[]{null, "UnknownTown", null}
        );

        when(accRepo.countDistinctAccountsWithConditionsGroupedByMunicipality()).thenReturn(rows);
        when(geo.findCodeByMunicipalityName("UnknownTown")).thenReturn("010");

        var res = svc.getCommunitymemberCountsByMunicipality();

        assertEquals(2, res.size());
        var city = res.stream().filter(r -> "City".equals(r.municipality())).findFirst().orElseThrow();
        assertEquals("10", city.municipalityCode());
        assertEquals(7L, city.amountOfCommunitymembers());

        var unk = res.stream().filter(r -> "UnknownTown".equals(r.municipality())).findFirst().orElseThrow();
        assertEquals("010", unk.municipalityCode());
        assertEquals(0L, unk.amountOfCommunitymembers());
    }
}
