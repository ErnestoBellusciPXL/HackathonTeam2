package be.codeforbelgium.openinzichten.config;

import be.codeforbelgium.openinzichten.domain.Zipcode;
import be.codeforbelgium.openinzichten.repository.ZipcodeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ZipcodeDataLoaderTests {

    @Test
    void loadZipcodes_alreadyHasData_skips() throws Exception {
        ZipcodeRepository repo = mock(ZipcodeRepository.class);
        when(repo.count()).thenReturn(5L);
        ZipcodeDataLoader loader = new ZipcodeDataLoader();
        var runner = loader.loadZipcodes(repo, "data/zipcodes.csv");
        runner.run();
        verify(repo, never()).save(any());
    }

    @Test
    void loadZipcodes_missingResource_skips() throws Exception {
        ZipcodeRepository repo = mock(ZipcodeRepository.class);
        when(repo.count()).thenReturn(0L);
        ZipcodeDataLoader loader = new ZipcodeDataLoader();
        var runner = loader.loadZipcodes(repo, "data/does_not_exist.csv");
        runner.run();
        verify(repo, never()).save(any());
    }

    @Test
    void loadZipcodes_withHeader_importsRows() throws Exception {
        ZipcodeRepository repo = mock(ZipcodeRepository.class);
        when(repo.count()).thenReturn(0L);
        ZipcodeDataLoader loader = new ZipcodeDataLoader();
        var runner = loader.loadZipcodes(repo, "data/zipcodes.csv");
        runner.run();

        ArgumentCaptor<Zipcode> captor = ArgumentCaptor.forClass(Zipcode.class);
        verify(repo, times(2)).save(captor.capture());
        var saved = captor.getAllValues();

        Zipcode z1 = saved.get(0);
        assertEquals("1000", z1.getCode());
        assertEquals("Brussel", z1.getGemeente());
        assertEquals("Brussel", z1.getProvincie());
        assertEquals("Brussels Gewest", z1.getGewest());

        Zipcode z2 = saved.get(1);
        assertEquals("2000", z2.getCode());
        assertEquals("Antwerpen", z2.getGemeente());
        assertEquals("Antwerpen", z2.getProvincie());
        assertEquals("Vlaanderen", z2.getGewest());
    }

    @Test
    void loadZipcodes_withoutHeader_importsRows() throws Exception {
        ZipcodeRepository repo = mock(ZipcodeRepository.class);
        when(repo.count()).thenReturn(0L);
        ZipcodeDataLoader loader = new ZipcodeDataLoader();
        var runner = loader.loadZipcodes(repo, "data/zipcodes_noheader.csv");
        runner.run();

        ArgumentCaptor<Zipcode> captor = ArgumentCaptor.forClass(Zipcode.class);
        verify(repo, times(2)).save(captor.capture());
        var saved = captor.getAllValues();

        Zipcode z1 = saved.get(0);
        assertEquals("3000", z1.getCode());
        assertEquals("Leuven", z1.getGemeente());
        assertEquals("Vlaams-Brabant", z1.getProvincie());
        assertEquals("Vlaanderen", z1.getGewest());

        Zipcode z2 = saved.get(1);
        assertEquals("3500", z2.getCode());
        assertEquals("Hasselt", z2.getGemeente());
        assertEquals("Limburg", z2.getProvincie());
        assertEquals("Vlaanderen", z2.getGewest());
    }
}
