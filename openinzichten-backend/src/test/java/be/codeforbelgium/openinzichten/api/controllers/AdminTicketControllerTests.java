package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.AdminTicketResponse;
import be.codeforbelgium.openinzichten.service.AdminTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminTicketControllerTests {

    private AdminTicketService adminTicketService;
    private AdminTicketController controller;

    @BeforeEach
    void setUp() {
        adminTicketService = mock(AdminTicketService.class);
        controller = new AdminTicketController(adminTicketService);
    }

    @Test
    void getTickets_returnsPagedResult() {
        AdminTicketResponse resp = new AdminTicketResponse();
        Page<AdminTicketResponse> page = new PageImpl<>(List.of(resp));
        when(adminTicketService.getAllTickets(1, 5)).thenReturn(page);

        ResponseEntity<Page<AdminTicketResponse>> response = controller.getTickets(1, 5);

        assertEquals(200, response.getStatusCode().value());
        assertSame(page, response.getBody());
        verify(adminTicketService).getAllTickets(1, 5);
    }

    @Test
    void getTicketById_returnsTicket() {
        AdminTicketResponse resp = new AdminTicketResponse();
        when(adminTicketService.getTicket("abc")).thenReturn(resp);

        ResponseEntity<AdminTicketResponse> response = controller.getTicketById("abc");

        assertEquals(200, response.getStatusCode().value());
        assertSame(resp, response.getBody());
        verify(adminTicketService).getTicket("abc");
    }

    @Test
    void closeTicket_callsServiceWithParams() {
        AdminTicketResponse resp = new AdminTicketResponse();
        when(adminTicketService.closeTicket(eq("id-1"), anyBoolean())).thenReturn(resp);

        ResponseEntity<AdminTicketResponse> response = controller.closeTicket("id-1", true);

        assertEquals(200, response.getStatusCode().value());
        assertSame(resp, response.getBody());
        verify(adminTicketService).closeTicket("id-1", true);
    }
}
