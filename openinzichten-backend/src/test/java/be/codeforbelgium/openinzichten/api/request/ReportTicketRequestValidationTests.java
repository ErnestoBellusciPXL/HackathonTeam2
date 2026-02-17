package be.codeforbelgium.openinzichten.api.request;

import be.codeforbelgium.openinzichten.domain.ReportReason;
import be.codeforbelgium.openinzichten.domain.TicketType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTicketRequestValidationTests {

    private static ValidatorFactory vf;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        vf = Validation.buildDefaultValidatorFactory();
        validator = vf.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (vf != null) vf.close();
    }

    @Test
    void storyType_withoutStoryId_reportsViolation() {
        ReportTicketRequest req = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, null, null);

        Set<ConstraintViolation<ReportTicketRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty());
        boolean found = violations.stream().anyMatch(v -> v.getMessage().contains("storyId is required when type is STORY"));
        assertTrue(found, "Expected violation message about missing storyId for STORY type");
    }

    @Test
    void storyType_withBlankStoryId_reportsViolation() {
        ReportTicketRequest req = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, "   ", null);

        Set<ConstraintViolation<ReportTicketRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty());
        boolean found = violations.stream().anyMatch(v -> v.getMessage().contains("storyId is required when type is STORY"));
        assertTrue(found);
    }

    @Test
    void storyType_withStoryId_noViolation() {
        ReportTicketRequest req = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, "story-1", null);

        Set<ConstraintViolation<ReportTicketRequest>> violations = validator.validate(req);

        assertTrue(violations.stream().noneMatch(v -> v.getMessage().contains("storyId is required when type is STORY")));
    }

    @Test
    void nullType_doesNotEnforceStoryId() {
        ReportTicketRequest req = new ReportTicketRequest(null, ReportReason.SPAM, null, null);

        Set<ConstraintViolation<ReportTicketRequest>> violations = validator.validate(req);

        // other constraints may still apply, but storyId rule should not fire
        boolean storyViolation = violations.stream().anyMatch(v -> v.getMessage().contains("storyId is required when type is STORY"));
        assertFalse(storyViolation);
    }

    @Test
    void otherReason_required_whenReportReasonOther() {
        ReportTicketRequest req = new ReportTicketRequest(TicketType.STORY, ReportReason.OTHER, "story-1", "   ");

        Set<ConstraintViolation<ReportTicketRequest>> violations = validator.validate(req);

        assertFalse(violations.isEmpty());
        boolean found = violations.stream().anyMatch(v -> v.getMessage().contains("otherReason must be provided when reportReason is OTHER"));
        assertTrue(found);
    }

    @Test
    void otherReason_present_whenReportReasonOther_noViolation() {
        ReportTicketRequest req = new ReportTicketRequest(TicketType.STORY, ReportReason.OTHER, "story-1", "Some reason");

        Set<ConstraintViolation<ReportTicketRequest>> violations = validator.validate(req);

        boolean otherViolation = violations.stream().anyMatch(v -> v.getMessage().contains("otherReason must be provided when reportReason is OTHER"));
        assertFalse(otherViolation);
    }
}
