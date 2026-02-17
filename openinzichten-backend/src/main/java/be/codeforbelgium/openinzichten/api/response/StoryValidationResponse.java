package be.codeforbelgium.openinzichten.api.response;

import lombok.Data;
import java.util.List;

@Data
public class StoryValidationResponse {
    private String fixedTitle;
    private String fixedContent;
    private List<StoryChange> changes;

    @Data
    public static class StoryChange {
        private String reason;
        private String description;
    }
}
