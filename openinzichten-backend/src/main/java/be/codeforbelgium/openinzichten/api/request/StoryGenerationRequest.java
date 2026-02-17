package be.codeforbelgium.openinzichten.api.request;

import lombok.Data;
import java.util.List;

@Data
public class StoryGenerationRequest {
    private List<QuestionAnswer> inputs;

    @Data
    public static class QuestionAnswer {
        private String question;
        private String answer;
    }
}
