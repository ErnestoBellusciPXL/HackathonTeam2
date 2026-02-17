package be.codeforbelgium.openinzichten.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminTicketResponse {
    private String id;
    private String type;
    private String state;
    private String reportReason;
    private String otherReason;
    private String reporterId;
    private String reporterUsername;
    private String reporteeId;
    private String reporteeUsername;
    private String storyId;
    private String storyTitle;
    private String storyTitleSnapshot;
    private String storyContentSnapshot;
    private String storyConditionsSnapshot;
    private Instant createdAt;
}
