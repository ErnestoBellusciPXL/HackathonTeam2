package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.AdminTicketResponse;
import be.codeforbelgium.openinzichten.domain.Story;
import be.codeforbelgium.openinzichten.domain.Ticket;
import be.codeforbelgium.openinzichten.domain.TicketState;
import be.codeforbelgium.openinzichten.exceptions.TicketNotFoundException;
import be.codeforbelgium.openinzichten.repository.StoryRepository;
import be.codeforbelgium.openinzichten.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTicketService {

    private final TicketRepository ticketRepository;
    private final StoryRepository storyRepository;

    public Page<AdminTicketResponse> getAllTickets(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        return ticketRepository.findAll(pageable)
                .map(this::mapToAdminResponse);
    }

    public AdminTicketResponse getTicket(String ticketId) {
        UUID uuid = parseUuid(ticketId, "Invalid ticket id");
        Ticket ticket = ticketRepository.findById(uuid)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        return mapToAdminResponse(ticket);
    }

    @Transactional
    public AdminTicketResponse closeTicket(String ticketId, boolean removeStory) {
        UUID uuid = parseUuid(ticketId, "Invalid ticket id");
        Ticket ticket = ticketRepository.findById(uuid)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (removeStory && ticket.getStory() != null) {
            Story story = ticket.getStory();
            List<Ticket> ticketsForStory = ticketRepository.findAllByStoryId(story.getId());
            ticketsForStory.forEach(t -> {
                t.setState(TicketState.CLOSED);
                t.setStory(null); // detach before deletion to avoid FK issues
            });
            ticketRepository.saveAll(ticketsForStory);
            storyRepository.delete(story);
            return mapToAdminResponse(ticket);
        }

        ticket.setState(TicketState.CLOSED);
        Ticket saved = ticketRepository.save(ticket);

        return mapToAdminResponse(saved);
    }

    private AdminTicketResponse mapToAdminResponse(Ticket ticket) {
        String reporterId = ticket.getReporter() != null && ticket.getReporter().getId() != null
                ? ticket.getReporter().getId().toString()
                : null;
        String reporterUsername = ticket.getReporter() != null
                ? ticket.getReporter().getUsername()
                : null;
        String reporteeId = ticket.getReportee() != null && ticket.getReportee().getId() != null
                ? ticket.getReportee().getId().toString()
                : null;
        String reporteeUsername = ticket.getReportee() != null
                ? ticket.getReportee().getUsername()
                : null;
        String storyId = ticket.getStory() != null && ticket.getStory().getId() != null
                ? ticket.getStory().getId().toString()
                : null;
        String storyTitle = ticket.getStory() != null
                ? ticket.getStory().getTitle()
                : null;

        return new AdminTicketResponse(
                ticket.getId() != null ? ticket.getId().toString() : null,
                ticket.getType() != null ? ticket.getType().name() : null,
                ticket.getState() != null ? ticket.getState().name() : null,
                ticket.getReportReason() != null ? ticket.getReportReason().name() : null,
                ticket.getOtherReason(),
                reporterId,
                reporterUsername,
                reporteeId,
                reporteeUsername,
                storyId,
                storyTitle,
                ticket.getStoryTitleSnapshot(),
                ticket.getStoryContentSnapshot(),
                ticket.getStoryConditionsSnapshot(),
                ticket.getCreatedAt());
    }

    private UUID parseUuid(String raw, String errorMessage) {
        try {
            return UUID.fromString(raw);
        } catch (Exception ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
