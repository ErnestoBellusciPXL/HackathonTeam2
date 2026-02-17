package be.codeforbelgium.openinzichten.api.request;

import lombok.Data;

@Data
public class StoryValidationRequest {
    private String title;
    private String content;
}
