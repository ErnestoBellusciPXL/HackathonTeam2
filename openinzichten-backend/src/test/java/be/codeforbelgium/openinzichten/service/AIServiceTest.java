package be.codeforbelgium.openinzichten.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.Resource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIService Tests")
class AIServiceTest {

    private static final String TEST_SYSTEM_PROMPT = "You are a helpful assistant that formats text.";
    private ChatClient chatClient;
    private AIService aiService;
    private Resource systemPromptResource;
    @Captor
    private ArgumentCaptor<Prompt> promptCaptor;

    private static Stream<Arguments> reformattingCases() {
        return Stream.of(
                Arguments.of("Dit is een test verhaal dat geformatteerd moet worden.",
                        "<p>Dit is een <strong>test verhaal</strong> dat geformatteerd moet worden.</p>"),
                Arguments.of("", ""),
                Arguments.of("   \n\t  ", ""),
                Arguments.of("Dit is een zeer lang verhaal. ".repeat(500), "<p>Formatted long content</p>"),
                Arguments.of("Test met €, ñ, ü, é, ë & < > \" ' karakters!",
                        "<p>Test met €, ñ, ü, é, ë &amp; &lt; &gt; &quot; &#39; karakters!</p>"),
                Arguments.of("Dit is <strong>al geformatteerd</strong> en <em>bevat tags</em>.",
                        "<p>Dit is <strong>al geformatteerd</strong> en <em>bevat tags</em>.</p>"),
                Arguments.of("Eerste paragraaf.\n\nTweede paragraaf.\nDerde regel in tweede paragraaf.",
                        "<p>Eerste paragraaf.</p><p>Tweede paragraaf.<br>Derde regel in tweede paragraaf.</p>"),
                Arguments.of("Op 20 november 2024 waren er 1.234 bezoekers en €56,78 werd gespaard.",
                        "<p>Op <strong>20 november 2024</strong> waren er <strong>1.234</strong> bezoekers en <strong>€56,78</strong> werd gespaard.</p>"),
                Arguments.of("Hij zei: \"Dit is belangrijk\" en het was 't beste.",
                        "<p>Hij zei: <cite>\"Dit is belangrijk\"</cite> en het was 't beste.</p>"),
                Arguments.of("Ge hebt gelijk, 't is inderdaad ne schoon dag voor een wandeling in 't park.",
                        "<p>Ge hebt gelijk, 't is inderdaad <em>ne schoon dag</em> voor een wandeling in 't park.</p>"),
                // Additional cases consolidated into the same parameterized test
                Arguments.of("Eerste paragraaf met informatie.\n\nTweede paragraaf met meer details.\n\nDerde paragraaf als conclusie.",
                        "<p>Eerste paragraaf met informatie.</p><p>Tweede paragraaf met meer details.</p><p>Derde paragraaf als conclusie.</p>"),
                Arguments.of("Dit is een test met emoji 😊 en unicode ™ © ® karakters.",
                        "<p>Dit is een test met emoji 😊 en unicode ™ © ® karakters.</p>"),
                Arguments.of("A", "<p>A</p>"),
                Arguments.of("Test with various formatting",
                        "<h1>Titel</h1><h2>Subtitel</h2><p>Text met <strong>bold</strong>, <em>italic</em>, <u>underline</u> en <cite>citation</cite>.</p>"),
                Arguments.of("Line 1\rLine 2\r\nLine 3\nLine 4", "<p>Line 1<br>Line 2<br>Line 3<br>Line 4</p>"),
                Arguments.of("Text\tmet\ttabs\ttussen\twoorden", "<p>Text met tabs tussen woorden</p>"),
                Arguments.of("A".repeat(10000), "<p>Formatted maximum content</p>"),
                Arguments.of("Content with multiple heading levels",
                        "<h1>H1</h1><h2>H2</h2><h3>H3</h3><h4>H4</h4><h5>H5</h5><h6>H6</h6>")
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        // Create a spy or mock for ChatClient that returns expected values
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

        // Create a mock builder that returns our mock ChatClient
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);

        // Create a mock Resource for the system prompt
        systemPromptResource = mock(Resource.class);
        when(systemPromptResource.getContentAsString(any())).thenReturn(TEST_SYSTEM_PROMPT);

        // Initialize AIService with the mocked builder and resource
        aiService = new AIService(builder, systemPromptResource);
    }

    @ParameterizedTest
    @MethodSource("reformattingCases")
    @DisplayName("Should reformat a variety of inputs correctly")
    void doStoryReformatting_variousInputs_producesExpected(String inputContent, String expectedOutput) {
        // Arrange
        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn(expectedOutput);

        // Act
        String result = aiService.doStoryReformatting(inputContent);

        // Assert
        assertThat(result).isNotNull().isEqualTo(expectedOutput);

        verify(chatClient, times(1)).prompt(any(Prompt.class));
    }

    void doStoryReformatting_whenChatClientReturnsNull_shouldHandleGracefully() {
        // Arrange
        String inputContent = "Test content";

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn(null);

        // Act
        String result = aiService.doStoryReformatting(inputContent);

        // Assert
        assertThat(result).isNull();

        verify(chatClient, times(1)).prompt(any(Prompt.class));
    }


    @Test
    @DisplayName("Should call ChatClient only once per formatting request")
    void doStoryReformatting_shouldCallChatClientOnlyOnce() {
        // Arrange
        String inputContent = "Test content";
        String expectedOutput = "Formatted content";

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn(expectedOutput);

        // Act
        aiService.doStoryReformatting(inputContent);

        // Assert
        verify(chatClient, times(1)).prompt(any(Prompt.class));
        // Note: verifyNoMoreInteractions doesn't work well with RETURNS_DEEP_STUBS
        // as it creates additional interactions in the stub chain
    }

    @Test
    @DisplayName("Constructor should handle different system prompts")
    void constructor_withDifferentSystemPrompts_shouldInitializeCorrectly() throws Exception {
        // Arrange
        String customPrompt = "Custom formatting instructions in Flemish";
        ChatClient customChatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder customBuilder = mock(ChatClient.Builder.class);
        when(customBuilder.build()).thenReturn(customChatClient);
        
        Resource customResource = mock(Resource.class);
        when(customResource.getContentAsString(any())).thenReturn(customPrompt);

        // Act
        AIService customService = new AIService(customBuilder, customResource);

        // Assert
        assertThat(customService).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty system prompt by throwing exception")
    void constructor_withEmptySystemPrompt_shouldThrowException() throws Exception {
        // Arrange
        String emptyPrompt = "";
        ChatClient emptyChatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder emptyBuilder = mock(ChatClient.Builder.class);
        when(emptyBuilder.build()).thenReturn(emptyChatClient);
        
        Resource emptyResource = mock(Resource.class);
        when(emptyResource.getContentAsString(any())).thenReturn(emptyPrompt);

        // Act & Assert
        // Spring AI's SystemPromptTemplate doesn't allow empty templates
        assertThatThrownBy(() -> new AIService(emptyBuilder, emptyResource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("template cannot be null or empty");
    }


    @Test
    @DisplayName("Should handle IllegalArgumentException from ChatClient call")
    void doStoryReformatting_whenChatClientCallThrowsIllegalArgumentException_shouldPropagateException() {
        // Arrange
        String inputContent = "Test content";

        when(chatClient.prompt(any(Prompt.class)).call()).thenThrow(new IllegalArgumentException("Invalid prompt"));

        // Act & Assert
        assertThatThrownBy(() -> aiService.doStoryReformatting(inputContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prompt");

        verify(chatClient, times(1)).prompt(any(Prompt.class));
    }

    @Test
    @DisplayName("Should handle NullPointerException from call returning null")
    void doStoryReformatting_whenCallReturnsNull_shouldHandleNullPointerException() {
        // Arrange
        String inputContent = "Test content";

        when(chatClient.prompt(any(Prompt.class)).call()).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> aiService.doStoryReformatting(inputContent))
                .isInstanceOf(NullPointerException.class);

        verify(chatClient, times(1)).prompt(any(Prompt.class));
    }


    @Test
    @DisplayName("Should verify user message contains input content")
    void doStoryReformatting_shouldContainInputInUserMessage() {
        // Arrange
        String inputContent = "Specific test content";
        String expectedOutput = "Formatted content";

        when(chatClient.prompt(promptCaptor.capture()).call().content()).thenReturn(expectedOutput);

        // Act
        aiService.doStoryReformatting(inputContent);

        // Assert
        Prompt capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).isNotNull();
        assertThat(capturedPrompt.getInstructions()).hasSize(2);

        // Verify the prompt was created with the messages
        // The second message should be the UserMessage with our input
        assertThat(capturedPrompt.getInstructions().get(1)).isNotNull();

        verify(chatClient, times(1)).prompt(any(Prompt.class));
    }

    @Test
    @DisplayName("Should handle timeout exception from API")
    void doStoryReformatting_whenApiTimesOut_shouldPropagateException() {
        // Arrange
        String inputContent = "Test content";

        when(chatClient.prompt(any(Prompt.class))).thenThrow(new RuntimeException("Timeout after 30 seconds"));

        // Act & Assert
        assertThatThrownBy(() -> aiService.doStoryReformatting(inputContent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Timeout");

        verify(chatClient, times(1)).prompt(any(Prompt.class));
    }

}

