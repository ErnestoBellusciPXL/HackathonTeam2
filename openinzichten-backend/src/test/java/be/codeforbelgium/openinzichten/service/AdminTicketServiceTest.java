package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.AdminTicketResponse;
import be.codeforbelgium.openinzichten.domain.*;
import be.codeforbelgium.openinzichten.exceptions.TicketNotFoundException;
import be.codeforbelgium.openinzichten.repository.StoryRepository;
import be.codeforbelgium.openinzichten.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AdminTicketServiceTest {

    private TicketRepository ticketRepository;
    private StoryRepository storyRepository;
    private AdminTicketService adminTicketService;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        storyRepository = mock(StoryRepository.class);
        adminTicketService = new AdminTicketService(ticketRepository, storyRepository);
    }

    @Test
    void closeTicket_whenRemovingStory_detachesAllTicketsBeforeDelete() {
        UUID ticketId = UUID.randomUUID();
        UUID storyId = UUID.randomUUID();

        Account reporter = Account.builder().id(UUID.randomUUID()).username("reporter").build();
        Account reportee = Account.builder().id(UUID.randomUUID()).username("owner").build();
        Story story = Story.builder()
                .id(storyId)
                .title("Story title")
                .content("Story content that is sufficiently long to satisfy validators.")
                .owner(reportee)
                .build();

        Ticket mainTicket = Ticket.builder()
                .id(ticketId)
                .type(TicketType.STORY)
                .state(TicketState.OPEN)
                .reportReason(ReportReason.SPAM)
                .reporter(reporter)
                .reportee(reportee)
                .story(story)
                .build();

        Ticket otherTicket = Ticket.builder()
                .id(UUID.randomUUID())
                .type(TicketType.STORY)
                .state(TicketState.OPEN)
                .reportReason(ReportReason.SPAM)
                .reporter(reporter)
                .reportee(reportee)
                .story(story)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(mainTicket));
        when(ticketRepository.findAllByStoryId(storyId)).thenReturn(List.of(mainTicket, otherTicket));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminTicketResponse response = adminTicketService.closeTicket(ticketId.toString(), true);

        assertEquals(TicketState.CLOSED.name(), response.getState());
        assertNull(mainTicket.getStory());
        assertNull(otherTicket.getStory());
        assertEquals(TicketState.CLOSED, otherTicket.getState());
        verify(storyRepository).delete(story);
        verify(ticketRepository).saveAll(anyList());
    }

    @Test
    void closeTicket_withoutRemovingStory_closesOnlyRequestedTicket() {
        UUID ticketId = UUID.randomUUID();
        Account reporter = Account.builder().id(UUID.randomUUID()).username("reporter").build();
        Account reportee = Account.builder().id(UUID.randomUUID()).username("owner").build();
        Story story = Story.builder()
                .id(UUID.randomUUID())
                .title("Title")
                .content("Content that is sufficiently long to satisfy validators.")
                .owner(reportee)
                .build();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .type(TicketType.STORY)
                .state(TicketState.OPEN)
                .reportReason(ReportReason.SPAM)
                .reporter(reporter)
                .reportee(reportee)
                .story(story)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        AdminTicketResponse response = adminTicketService.closeTicket(ticketId.toString(), false);

        assertEquals(TicketState.CLOSED.name(), response.getState());
        assertEquals(story, ticket.getStory());
        verify(ticketRepository).save(ticket);
        verifyNoInteractions(storyRepository);
    }

    @Test
    void getTicket_withInvalidId_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> adminTicketService.getTicket("bad-id"));
    }

    @Test
    void getAllTickets_mapsAdminResponseFields() {
        UUID ticketId = UUID.randomUUID();
        UUID storyId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .type(TicketType.STORY)
                .state(TicketState.OPEN)
                .reportReason(ReportReason.SPAM)
                .otherReason("other")
                .reporter(Account.builder().id(UUID.randomUUID()).username("reporter").build())
                .reportee(Account.builder().id(UUID.randomUUID()).username("reportee").build())
                .story(Story.builder().id(storyId).title("title").build())
                .storyTitleSnapshot("snapshot title")
                .storyContentSnapshot("snapshot content")
                .storyConditionsSnapshot("cond1,cond2")
                .createdAt(java.time.Instant.now())
                .build();

        when(ticketRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(ticket)));

        var page = adminTicketService.getAllTickets(0, 10);

        assertEquals(1, page.getTotalElements());
        AdminTicketResponse response = page.getContent().get(0);
        assertEquals(ticketId.toString(), response.getId());
        assertEquals("snapshot title", response.getStoryTitleSnapshot());
        assertEquals("snapshot content", response.getStoryContentSnapshot());
        assertEquals("cond1,cond2", response.getStoryConditionsSnapshot());
    }

    @Test
    void getTicket_whenExists_mapsFields() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .type(TicketType.STORY)
                .state(TicketState.OPEN)
                .reportReason(ReportReason.SPAM)
                .otherReason("other")
                .reporter(Account.builder().id(UUID.randomUUID()).username("reporter").build())
                .reportee(Account.builder().id(UUID.randomUUID()).username("reportee").build())
                .story(Story.builder().id(UUID.randomUUID()).title("title").build())
                .storyTitleSnapshot("snapshot title")
                .storyContentSnapshot("snapshot content")
                .storyConditionsSnapshot("cond1,cond2")
                .createdAt(java.time.Instant.now())
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        AdminTicketResponse response = adminTicketService.getTicket(ticketId.toString());

        assertEquals(ticketId.toString(), response.getId());
        assertEquals("snapshot title", response.getStoryTitleSnapshot());
        assertEquals("snapshot content", response.getStoryContentSnapshot());
        assertEquals("cond1,cond2", response.getStoryConditionsSnapshot());
    }

    @Test
    void getTicket_notFound_throwsTicketNotFoundException() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        String idStr = ticketId.toString();
        assertThrows(TicketNotFoundException.class, () -> adminTicketService.getTicket(idStr));
    }

    @Test
    void mapToAdminResponse_handlesNullFields() {
        Ticket ticket = new Ticket();
        ticket.setId(null);
        ticket.setType(null);
        ticket.setState(null);
        ticket.setReportReason(null);
        ticket.setOtherReason(null);
        ticket.setReporter(null);
        ticket.setReportee(null);
        ticket.setStory(null);
        ticket.setStoryTitleSnapshot(null);
        ticket.setStoryContentSnapshot(null);
        ticket.setStoryConditionsSnapshot(null);
        ticket.setCreatedAt(null);

        // Use reflection via public API by stubbing repository to return the ticket
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));

        // calling getTicket will parse uuid then map; use a random uuid string but stub repository accordingly
        // we'll call mapToAdminResponse indirectly by invoking closeTicket to get a mapped response for nulls
        // create a ticket id string that is a parsable UUID
        // instead, call the private mapping by saving and fetching via getAllTickets page mapping
        when(ticketRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(ticket)));

        var page = adminTicketService.getAllTickets(0, 1);
        AdminTicketResponse response = page.getContent().get(0);

        assertNull(response.getId());
        assertNull(response.getType());
        assertNull(response.getState());
        assertNull(response.getReportReason());
        assertNull(response.getOtherReason());
        assertNull(response.getReporterId());
        assertNull(response.getReporterUsername());
        assertNull(response.getReporteeId());
        assertNull(response.getReporteeUsername());
        assertNull(response.getStoryId());
        assertNull(response.getStoryTitle());
        assertNull(response.getStoryTitleSnapshot());
        assertNull(response.getStoryContentSnapshot());
        assertNull(response.getStoryConditionsSnapshot());
        assertNull(response.getCreatedAt());
    }
}
