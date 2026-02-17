package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.AdminTicketResponse;
import be.codeforbelgium.openinzichten.service.AdminTicketService;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tickets")
@Validated
public class AdminTicketController {

    private final AdminTicketService adminTicketService;

    @GetMapping
    public ResponseEntity<Page<AdminTicketResponse>> getTickets(
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "page must be zero or positive") int page,
            @RequestParam(defaultValue = "10") @Positive(message = "pageSize must be positive") int pageSize) {
        Page<AdminTicketResponse> tickets = adminTicketService.getAllTickets(page, pageSize);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<AdminTicketResponse> getTicketById(@PathVariable String ticketId) {
        AdminTicketResponse ticket = adminTicketService.getTicket(ticketId);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{ticketId}/close")
    public ResponseEntity<AdminTicketResponse> closeTicket(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "false") boolean removeStory) {
        AdminTicketResponse ticket = adminTicketService.closeTicket(ticketId, removeStory);
        return ResponseEntity.ok(ticket);
    }
}
