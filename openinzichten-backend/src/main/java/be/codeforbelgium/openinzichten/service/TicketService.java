package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.request.ReportTicketRequest;
import be.codeforbelgium.openinzichten.api.response.TicketResponse;
import be.codeforbelgium.openinzichten.domain.*;
import be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException;
import be.codeforbelgium.openinzichten.exceptions.StoryNotFoundException;
import be.codeforbelgium.openinzichten.exceptions.TicketAlreadyExistsException;
import be.codeforbelgium.openinzichten.exceptions.TicketNotFoundException;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.StoryRepository;
import be.codeforbelgium.openinzichten.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AccountRepository accountRepository;
    private final StoryRepository storyRepository;

    public TicketResponse createTicket(String reporterUsername, ReportTicketRequest request) {
        if (request.getType() != TicketType.STORY) {
            throw new IllegalArgumentException("Unsupported ticket type: " + request.getType());
        }

        Account reporter = accountRepository.findByUsername(reporterUsername)
                .orElseThrow(() -> new AccountNotFoundException(reporterUsername));

        UUID storyId = parseUuid(request.getStoryId(), "Invalid story id");
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(request.getStoryId()));

        if (ticketRepository.existsByReporterIdAndStoryId(reporter.getId(), storyId)) {
            throw new TicketAlreadyExistsException("This story is already reported by this user");
        }

        Account reportee = story.getOwner();
        if (reportee == null) {
            throw new IllegalStateException("Story has no owner");
        }

        String otherReason = null;
        if (request.getReportReason() == ReportReason.OTHER) {
            if (request.getOtherReason() == null || request.getOtherReason().isBlank()) {
                throw new IllegalArgumentException("otherReason is required when reportReason is OTHER");
            }
            otherReason = request.getOtherReason().trim();
        }

        Ticket ticket = Ticket.builder()
                .reporter(reporter)
                .reportee(reportee)
                .type(request.getType())
                .reportReason(request.getReportReason())
                .otherReason(otherReason)
                .story(story)
                .storyTitleSnapshot(story.getTitle())
                .storyContentSnapshot(story.getContent())
                .storyConditionsSnapshot(extractConditionsSnapshot(story))
                .build();

        Ticket saved = ticketRepository.save(ticket);
        return mapToResponse(saved);
    }

    public Page<TicketResponse> getAllTickets(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        return ticketRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public TicketResponse updateTicketState(String ticketId, TicketState newState) {
        UUID uuid = parseUuid(ticketId, "Invalid ticket id");
        Ticket ticket = ticketRepository.findById(uuid)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        ticket.setState(newState);
        Ticket saved = ticketRepository.save(ticket);
        return mapToResponse(saved);
    }

    public TicketResponse mapToResponse(Ticket ticket) {
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

        return new TicketResponse(
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
                storyTitle);
    }

    private UUID parseUuid(String raw, String errorMessage) {
        try {
            return UUID.fromString(raw);
        } catch (Exception ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String extractConditionsSnapshot(Story story) {
        if (story == null || story.getConditions() == null) {
            return null;
        }
        return story.getConditions().stream()
                .map(Condition::getName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.joining(","));
    }
}
