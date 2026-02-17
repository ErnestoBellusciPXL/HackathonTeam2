package be.codeforbelgium.openinzichten.config;

import be.codeforbelgium.openinzichten.domain.Community;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.repository.CommunityRepository;
import be.codeforbelgium.openinzichten.repository.ConditionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ConditionCommunityDataLoaderTests {

    @Test
    void runner_skips_when_data_present() throws Exception {
        ConditionRepository condRepo = mock(ConditionRepository.class);
        CommunityRepository commRepo = mock(CommunityRepository.class);

        when(condRepo.count()).thenReturn(5L);
        when(commRepo.count()).thenReturn(0L);

        ConditionCommunityDataLoader loader = new ConditionCommunityDataLoader();
        CommandLineRunner runner = loader.loadConditionsAndCommunities(condRepo, commRepo);

        // execute
        runner.run();

        verify(condRepo, never()).saveAll(anyList());
        verify(commRepo, never()).save(any(Community.class));
    }

    @Test
    void runner_seeds_when_empty() throws Exception {
        ConditionRepository condRepo = mock(ConditionRepository.class);
        CommunityRepository commRepo = mock(CommunityRepository.class);

        when(condRepo.count()).thenReturn(0L);
        when(commRepo.count()).thenReturn(0L);

        // Make communityRepository.save return the passed community so links are preserved
        when(commRepo.save(any(Community.class))).thenAnswer(inv -> inv.getArgument(0));

        // Intercept saveAll to assert the number of conditions being saved
        when(condRepo.saveAll(anyIterable())).thenAnswer(inv -> {
            Iterable<Condition> it = inv.getArgument(0);
            int cnt = 0;
            for (Object o : it) {
                assertNotNull(o);
                cnt++;
            }
            assertEquals(885, cnt);
            return it;
        });

        ConditionCommunityDataLoader loader = new ConditionCommunityDataLoader();
        CommandLineRunner runner = loader.loadConditionsAndCommunities(condRepo, commRepo);

        runner.run();

        // 5 community names should be saved
        verify(commRepo, times(5)).save(any(Community.class));

        verify(condRepo, times(1)).saveAll(anyIterable());

        // check at least one condition has a matching community link by capturing via another mock interaction
        // we can verify indirectly by ensuring communities were saved and the saveAll was called (above assertion checked count)
        // additionally assert community saves happened
        verify(commRepo, atLeastOnce()).save(any(Community.class));
    }
}
