package be.codeforbelgium.openinzichten.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class AIService {

    private final ChatClient chatClient;
    private final Message systemPromptMessage;

    @Autowired
    public AIService(ChatClient.Builder chatClientBuilder, @Value("${app.ai.system-prompt-path}") Resource systemPromptResource) throws IOException {
        this.chatClient = chatClientBuilder.build();

        var systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);

        var systemPromptTemplate = new SystemPromptTemplate(systemPrompt);

        this.systemPromptMessage = systemPromptTemplate.createMessage();
    }

    public String doStoryReformatting(String storyContent) {
        var userMessage = new UserMessage(storyContent);

        Prompt prompt = new Prompt(systemPromptMessage, userMessage);

        return chatClient.prompt(prompt).call().content();
    }

}
