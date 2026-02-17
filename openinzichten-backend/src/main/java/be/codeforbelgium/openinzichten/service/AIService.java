package be.codeforbelgium.openinzichten.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import be.codeforbelgium.openinzichten.api.request.StoryGenerationRequest;
import be.codeforbelgium.openinzichten.api.response.StoryValidationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private final ChatClient chatClient;
    private final Message generateStorySystemPromptMessage;
    private final Message validateStorySystemPromptMessage;
    private final Message systemPromptMessage;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AIService(ChatClient.Builder chatClientBuilder,
                     @Value("${app.ai.system-prompt-path}") Resource systemPromptResource,
                     @Value("classpath:ai/system-prompt-story-generation.txt") Resource generateStorySystemPromptResource,
                     @Value("classpath:ai/system-prompt-story-validation.txt") Resource validateStorySystemPromptResource) throws IOException {
        this.chatClient = chatClientBuilder.build();

        // 1. Existing prompt
        var systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.systemPromptMessage = new SystemMessage(systemPrompt);

        // 2. Generate Story Prompt
        var genPrompt = generateStorySystemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.generateStorySystemPromptMessage = new SystemMessage(genPrompt);

        // 3. Validate Story Prompt
        var valPrompt = validateStorySystemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.validateStorySystemPromptMessage = new SystemMessage(valPrompt);
    }

    public String doStoryReformatting(String storyContent) {
        try {
            var userMessage = new UserMessage(storyContent);
            Prompt prompt = new Prompt(systemPromptMessage, userMessage);
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            log.error("Error in doStoryReformatting: {}", e.getMessage(), e);
            throw new RuntimeException("AI Story Reformatting failed", e);
        }
    }

    public String generateStory(List<StoryGenerationRequest.QuestionAnswer> inputs) {
        try {
            StringBuilder sb = new StringBuilder();
            for (var input : inputs) {
                sb.append("Vraag: ").append(input.getQuestion()).append("\n");
                sb.append("Antwoord: ").append(input.getAnswer()).append("\n\n");
            }

            var userMessage = new UserMessage(sb.toString());
            Prompt prompt = new Prompt(generateStorySystemPromptMessage, userMessage);
            
            log.info("Requesting story generation from AI...");
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            log.error("Error in generateStory: {}", e.getMessage(), e);
            throw new RuntimeException("AI Story Generation failed", e);
        }
    }

    public StoryValidationResponse validateAndFix(String title, String content) {
        try {
            String userContent = "Titel: " + title + "\n\nInhoud:\n" + content;
            var userMessage = new UserMessage(userContent);
            Prompt prompt = new Prompt(validateStorySystemPromptMessage, userMessage);
            
            log.info("Requesting story validation from AI...");
            String responseJson = chatClient.prompt(prompt).call().content();
            
            responseJson = cleanJson(responseJson);

            return objectMapper.readValue(responseJson, StoryValidationResponse.class);
        } catch (Exception e) {
            log.error("Error in validateAndFix: {}", e.getMessage(), e);
            throw new RuntimeException("AI Story Validation failed", e);
        }
    }

    private String cleanJson(String response) {
        if (response == null) return "{}";
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
