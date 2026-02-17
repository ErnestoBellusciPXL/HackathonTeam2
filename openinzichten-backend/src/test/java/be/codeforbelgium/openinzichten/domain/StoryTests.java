package be.codeforbelgium.openinzichten.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoryTests {

    @Test
    void builder_and_getters_work() {
        Story s = Story.builder()
                .title("My title")
                .content("Some content")
                .build();

        assertEquals("My title", s.getTitle());
        assertEquals("Some content", s.getContent());
    }
}
