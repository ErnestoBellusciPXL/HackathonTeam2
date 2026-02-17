package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.request.ReportTicketRequest;
import be.codeforbelgium.openinzichten.api.response.TicketResponse;
import be.codeforbelgium.openinzichten.domain.*;
import be.codeforbelgium.openinzichten.exceptions.TicketAlreadyExistsException;
import be.codeforbelgium.openinzichten.exceptions.TicketNotFoundException;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.StoryRepository;
import be.codeforbelgium.openinzichten.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketServiceTests {

    private TicketRepository ticketRepository;
    private AccountRepository accountRepository;
    private StoryRepository storyRepository;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        accountRepository = mock(AccountRepository.class);
        storyRepository = mock(StoryRepository.class);
        ticketService = new TicketService(ticketRepository, accountRepository, storyRepository);
    }

    @Test
    void createTicket_withValidStory_buildsOpenTicket() {
        UUID reporterId = UUID.randomUUID();
        UUID reporteeId = UUID.randomUUID();
        UUID storyId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        Account reporter = Account.builder().id(reporterId).username("reporter").build();
        Account owner = Account.builder().id(reporteeId).username("owner").build();
        Story story = Story.builder()
                .id(storyId)
                .title("Story title")
                .content("Story content that is long enough to be valid in normal flows.")
                .owner(owner)
                .build();

        when(accountRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(ticketRepository.existsByReporterIdAndStoryId(reporterId, storyId)).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setId(ticketId);
            return t;
        });

        ReportTicketRequest request = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, storyId.toString(), null);

        TicketResponse response = ticketService.createTicket("reporter", request);

        assertEquals(ticketId.toString(), response.getId());
        assertEquals("OPEN", response.getState());
        assertEquals("SPAM", response.getReportReason());
        assertEquals(storyId.toString(), response.getStoryId());
        assertEquals("reporter", response.getReporterUsername());
        assertEquals("owner", response.getReporteeUsername());

        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void createTicket_withMissingOtherReason_throwsIllegalArgument() {
        UUID storyId = UUID.randomUUID();
        Account reporter = Account.builder().id(UUID.randomUUID()).username("reporter").build();
        Account owner = Account.builder().id(UUID.randomUUID()).username("owner").build();
        Story story = Story.builder()
                .id(storyId)
                .title("Story title")
                .content("Story content that is long enough to be valid in normal flows.")
                .owner(owner)
                .build();

        when(accountRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(ticketRepository.existsByReporterIdAndStoryId(reporter.getId(), storyId)).thenReturn(false);

        ReportTicketRequest request = new ReportTicketRequest(TicketType.STORY, ReportReason.OTHER, storyId.toString(), null);

        assertThrows(IllegalArgumentException.class, () -> ticketService.createTicket("reporter", request));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void createTicket_whenDuplicateForStory_throwsTicketAlreadyExistsException() {
        UUID storyId = UUID.randomUUID();
        Account reporter = Account.builder().id(UUID.randomUUID()).username("reporter").build();
        Account owner = Account.builder().id(UUID.randomUUID()).username("owner").build();
        Story story = Story.builder()
                .id(storyId)
                .title("Story title")
                .content("Story content that is long enough to be valid in normal flows.")
                .owner(owner)
                .build();

        when(accountRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(ticketRepository.existsByReporterIdAndStoryId(reporter.getId(), storyId)).thenReturn(true);

        ReportTicketRequest request = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, storyId.toString(), null);

        assertThrows(TicketAlreadyExistsException.class, () -> ticketService.createTicket("reporter", request));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void createTicket_withUnsupportedType_throwsIllegalArgument() {
        ReportTicketRequest request = new ReportTicketRequest(null, ReportReason.SPAM, null, null);
        assertThrows(IllegalArgumentException.class, () -> ticketService.createTicket("reporter", request));
    }

    @Test
    void createTicket_whenStoryHasNoOwner_throwsIllegalState() {
        UUID storyId = UUID.randomUUID();
        Account reporter = Account.builder().id(UUID.randomUUID()).username("reporter").build();
        Story story = Story.builder()
                .id(storyId)
                .title("Story title")
                .content("Story content that is long enough to be valid in normal flows.")
                .owner(null)
                .build();

        when(accountRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(ticketRepository.existsByReporterIdAndStoryId(reporter.getId(), storyId)).thenReturn(false);

        ReportTicketRequest request = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, storyId.toString(), null);

        assertThrows(IllegalStateException.class, () -> ticketService.createTicket("reporter", request));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void createTicket_withInvalidStoryId_throwsIllegalArgument() {
        ReportTicketRequest request = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, "not-a-uuid", null);
        String reporterUsername = "reporter";
        assertThrows(be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException.class, () -> ticketService.createTicket(reporterUsername, request));
    }

    @Test
    void createTicket_withOtherReason_trimsAndCopiesSnapshots() {
        UUID storyId = UUID.randomUUID();
        Account reporter = Account.builder().id(UUID.randomUUID()).username("reporter").build();
        Account owner = Account.builder().id(UUID.randomUUID()).username("owner").build();
        Condition condA = new Condition();
        condA.setName("Beta");
        Condition condB = new Condition();
        condB.setName("alpha");
        Story story = Story.builder()
                .id(storyId)
                .title("Story title")
                .content("Story content that is long enough to be valid in normal flows.")
                .owner(owner)
                .conditions(new java.util.HashSet<>(java.util.List.of(condA, condB)))
                .build();

        when(accountRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(story));
        when(ticketRepository.existsByReporterIdAndStoryId(reporter.getId(), storyId)).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportTicketRequest request = new ReportTicketRequest(TicketType.STORY, ReportReason.OTHER, storyId.toString(), " reason with spaces ");

        ticketService.createTicket("reporter", request);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();
        assertEquals("reason with spaces", saved.getOtherReason());
        assertEquals("Story title", saved.getStoryTitleSnapshot());
        assertEquals("Story content that is long enough to be valid in normal flows.", saved.getStoryContentSnapshot());
        assertEquals("alpha,Beta", saved.getStoryConditionsSnapshot());
    }

    @Test
    void updateTicketState_whenFound_updatesState() {
        UUID ticketId = UUID.randomUUID();
        Account reporter = Account.builder().id(UUID.randomUUID()).username("reporter").build();
        Account owner = Account.builder().id(UUID.randomUUID()).username("owner").build();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .type(TicketType.STORY)
                .state(TicketState.OPEN)
                .reportReason(ReportReason.SPAM)
                .reporter(reporter)
                .reportee(owner)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.updateTicketState(ticketId.toString(), TicketState.CLOSED);

        assertEquals("CLOSED", response.getState());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void updateTicketState_whenMissing_throwsNotFound() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        String id = ticketId.toString();
        assertThrows(TicketNotFoundException.class, () -> ticketService.updateTicketState(id, TicketState.CLOSED));
    }

    @Test
    void updateTicketState_withInvalidId_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> ticketService.updateTicketState("not-a-uuid", TicketState.CLOSED));
    }

    @Test
    void mapToResponse_handlesNullFields() {
        Ticket ticket = new Ticket();
        TicketResponse response = ticketService.mapToResponse(ticket);

        assertNull(response.getId());
        assertNull(response.getType());
        assertNull(response.getReportReason());
        assertNull(response.getReporterId());
        assertNull(response.getReporteeId());
        assertNull(response.getStoryId());
    }

    @Test
    void getAllTickets_mapsPageResults() {
        UUID reporterId = UUID.randomUUID();
        UUID reporteeId = UUID.randomUUID();
        UUID storyId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .type(TicketType.STORY)
                .state(TicketState.OPEN)
                .reportReason(ReportReason.SPAM)
                .reporter(Account.builder().id(reporterId).username("reporter").build())
                .reportee(Account.builder().id(reporteeId).username("reportee").build())
                .story(Story.builder().id(storyId).title("Title").build())
                .build();
        when(ticketRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(ticket)));

        var result = ticketService.getAllTickets(0, 10);
        assertEquals(1, result.getTotalElements());
        TicketResponse resp = result.getContent().get(0);
        assertEquals(storyId.toString(), resp.getStoryId());
        assertEquals("reporter", resp.getReporterUsername());
    }
}
