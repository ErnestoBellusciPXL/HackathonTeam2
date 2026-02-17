package be.codeforbelgium.openinzichten.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketTests {

    @Test
    void builder_defaultsStateOpen_andCopiesSnapshots() {
        Account reporter = Account.builder().id(UUID.randomUUID()).username("rep").build();
        Account reportee = Account.builder().id(UUID.randomUUID()).username("ree").build();
        Story story = Story.builder().id(UUID.randomUUID()).title("title").content("content that is long enough to be valid in normal flows.").build();

        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .reporter(reporter)
                .reportee(reportee)
                .type(TicketType.STORY)
                .reportReason(ReportReason.SPAM)
                .story(story)
                .storyTitleSnapshot("snap title")
                .storyContentSnapshot("snap content")
                .storyConditionsSnapshot("c1,c2")
                .build();

        assertEquals(TicketState.OPEN, ticket.getState());
        assertEquals("snap title", ticket.getStoryTitleSnapshot());
        assertEquals("snap content", ticket.getStoryContentSnapshot());
        assertEquals("c1,c2", ticket.getStoryConditionsSnapshot());
    }

    @Test
    void onCreate_setsCreatedAtWhenNull_andDoesNotOverrideExistingValue() {
        Ticket ticket = new Ticket();
        assertNull(ticket.getCreatedAt());
        ticket.onCreate();
        assertNotNull(ticket.getCreatedAt());

        Instant preset = Instant.parse("2023-01-01T00:00:00Z");
        ticket.setCreatedAt(preset);
        ticket.onCreate();
        assertEquals(preset, ticket.getCreatedAt());
    }
}
