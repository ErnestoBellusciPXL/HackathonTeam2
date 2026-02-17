package be.codeforbelgium.openinzichten.api.request;

import be.codeforbelgium.openinzichten.domain.ReportReason;
import be.codeforbelgium.openinzichten.domain.TicketType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportTicketRequest {

    @NotNull(message = "type must be provided")
    private TicketType type;

    @NotNull(message = "reportReason must be provided")
    private ReportReason reportReason;

    private String storyId;

    @Size(max = 1000, message = "otherReason must be at most 1000 characters")
    private String otherReason;

    @AssertTrue(message = "storyId is required when type is STORY")
    public boolean isStoryIdPresentForStoryType() {
        if (type == null) {
            return true;
        }
        if (type == TicketType.STORY) {
            return storyId != null && !storyId.isBlank();
        }
        return true;
    }

    @AssertTrue(message = "otherReason must be provided when reportReason is OTHER")
    public boolean isOtherReasonPresentWhenOtherSelected() {
        if (reportReason == null) {
            return true;
        }
        if (reportReason == ReportReason.OTHER) {
            return otherReason != null && !otherReason.isBlank();
        }
        return true;
    }
}
