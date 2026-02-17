package be.codeforbelgium.openinzichten.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StoryLikeTests {

    @Test
    void builder_and_getters_return_values() {
        UUID id = UUID.randomUUID();
        Story story = new Story();
        Account account = new Account();
        Instant created = Instant.now();

        StoryLike s = StoryLike.builder()
                .id(id)
                .story(story)
                .account(account)
                .createdAt(created)
                .build();

        assertEquals(id, s.getId());
        assertSame(story, s.getStory());
        assertSame(account, s.getAccount());
        assertEquals(created, s.getCreatedAt());
    }

    @Test
    void onCreate_sets_createdAt_when_null() {
        StoryLike s = new StoryLike();
        s.setCreatedAt(null);
        assertNull(s.getCreatedAt());
        s.onCreate();
        assertNotNull(s.getCreatedAt());
        // createdAt should be recent (not in the future)
        assertFalse(s.getCreatedAt().isAfter(Instant.now()));
    }

    @Test
    void onCreate_leaves_existing_createdAt() {
        StoryLike s = new StoryLike();
        Instant fixed = Instant.parse("2020-01-01T00:00:00Z");
        s.setCreatedAt(fixed);
        s.onCreate();
        assertEquals(fixed, s.getCreatedAt());
    }

    @Test
    void equals_and_hashcode_based_on_id_only() {
        UUID id = UUID.randomUUID();
        StoryLike a = new StoryLike();
        StoryLike b = new StoryLike();
        a.setId(id);
        b.setId(id);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        StoryLike c = new StoryLike();
        c.setId(UUID.randomUUID());
        assertNotEquals(a, c);
    }
}
