package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.request.ReportTicketRequest;
import be.codeforbelgium.openinzichten.api.response.TicketResponse;
import be.codeforbelgium.openinzichten.domain.ReportReason;
import be.codeforbelgium.openinzichten.domain.TicketType;
import be.codeforbelgium.openinzichten.service.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketControllerTests {

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = mock(TicketService.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTicket_unauthenticated_returns401() {
        TicketController controller = new TicketController(ticketService);
        SecurityContextHolder.clearContext();

        ReportTicketRequest request = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, "story-id", null);

        var response = controller.createTicket(request);
        assertEquals(401, response.getStatusCode().value());
        assertNull(response.getBody());
        verifyNoInteractions(ticketService);
    }

    @Test
    void createTicket_authenticated_callsService() {
        TicketController controller = new TicketController(ticketService);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("alice");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        ReportTicketRequest request = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, "story-id", null);
        TicketResponse responseDto = new TicketResponse(
                "ticket-1",
                "STORY",
                "OPEN",
                "SPAM",
                null,
                "reporter-id",
                "alice",
                "reportee-id",
                "owner",
                "story-id",
                "Story title"
        );

        when(ticketService.createTicket(eq("alice"), any(ReportTicketRequest.class))).thenReturn(responseDto);

        var response = controller.createTicket(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDto, response.getBody());
        verify(ticketService).createTicket(eq("alice"), any(ReportTicketRequest.class));
    }

    @Test
    void createTicket_anonymousUser_returns401() {
        TicketController controller = new TicketController(ticketService);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("anonymousUser");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        ReportTicketRequest request = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, "story-id", null);

        var response = controller.createTicket(request);
        assertEquals(401, response.getStatusCode().value());
        assertNull(response.getBody());
        verifyNoInteractions(ticketService);
    }

    @Test
    void createTicket_isAuthenticatedFalse_returns401() {
        TicketController controller = new TicketController(ticketService);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("bob");
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        ReportTicketRequest request = new ReportTicketRequest(null, null, "story-id", null);

        var response = controller.createTicket(request);
        assertEquals(401, response.getStatusCode().value());
        assertNull(response.getBody());
        verifyNoInteractions(ticketService);
    }

    @Test
    void createTicket_blankName_returns401() {
        TicketController controller = new TicketController(ticketService);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("   ");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        ReportTicketRequest request = new ReportTicketRequest(null, null, "story-id", null);

        var response = controller.createTicket(request);
        assertEquals(401, response.getStatusCode().value());
        assertNull(response.getBody());
        verifyNoInteractions(ticketService);
    }
}
