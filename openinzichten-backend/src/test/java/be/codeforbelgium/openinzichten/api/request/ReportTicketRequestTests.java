package be.codeforbelgium.openinzichten.api.request;

import be.codeforbelgium.openinzichten.domain.ReportReason;
import be.codeforbelgium.openinzichten.domain.TicketType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportTicketRequestTests {

    @Test
    void gettersAndSetters_work() {
        ReportTicketRequest req = new ReportTicketRequest();
        req.setType(TicketType.STORY);
        req.setReportReason(ReportReason.SPAM);
        req.setStoryId("story-id");
        req.setOtherReason("other");

        assertEquals(TicketType.STORY, req.getType());
        assertEquals(ReportReason.SPAM, req.getReportReason());
        assertEquals("story-id", req.getStoryId());
        assertEquals("other", req.getOtherReason());
    }

    @Test
    void validation_requiresStoryIdForStoryType() {
        ReportTicketRequest req = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, null, null);
        assertFalse(req.isStoryIdPresentForStoryType());

        req.setStoryId("abc");
        assertTrue(req.isStoryIdPresentForStoryType());
    }

    @Test
    void validation_requiresOtherReasonWhenOtherSelected() {
        ReportTicketRequest req = new ReportTicketRequest(TicketType.STORY, ReportReason.OTHER, "story", null);
        assertFalse(req.isOtherReasonPresentWhenOtherSelected());

        req.setOtherReason(" details ");
        assertTrue(req.isOtherReasonPresentWhenOtherSelected());
    }

    @Test
    void validation_allowsNullTypeOrNonStoryWithoutStoryId() {
        ReportTicketRequest req = new ReportTicketRequest(null, ReportReason.SPAM, null, null);
        assertTrue(req.isStoryIdPresentForStoryType());

        req.setType(TicketType.STORY);
        assertFalse(req.isStoryIdPresentForStoryType());

        req.setType(TicketType.STORY);
        req.setStoryId("abc");
        assertTrue(req.isStoryIdPresentForStoryType());
    }

    @Test
    void validation_allowsNonOtherReasonWithoutOtherText() {
        ReportTicketRequest req = new ReportTicketRequest(TicketType.STORY, ReportReason.SPAM, "story", null);
        assertTrue(req.isOtherReasonPresentWhenOtherSelected());

        req.setReportReason(null);
        assertTrue(req.isOtherReasonPresentWhenOtherSelected());
    }
}
