package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.request.SendMessageRequest;
import be.codeforbelgium.openinzichten.api.response.ChatMessageResponse;
import be.codeforbelgium.openinzichten.api.response.ChatMessagesResponse;
import be.codeforbelgium.openinzichten.api.response.ChatQuickOverviewItemResponse;
import be.codeforbelgium.openinzichten.service.ChatService;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatControllerTests {

    @Test
    void sendMessage_success_returns200AndBody() {
        ChatService svc = mock(ChatService.class);
        ChatController controller = new ChatController(svc);
        UUID chatId = UUID.randomUUID();
        var expected = new ChatMessageResponse(UUID.randomUUID(), chatId, "alice", UUID.randomUUID(), "hi", Instant.now());
        when(svc.sendMessage("alice", chatId, "hi")).thenReturn(expected);

        SendMessageRequest req = new SendMessageRequest(chatId, "hi");
        Principal principal = () -> "alice";

        var resp = controller.sendMessage(req, principal);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(expected, resp.getBody());
    }

    @Test
    void getMessages_success_returns200AndList() {
        ChatService svc = mock(ChatService.class);
        ChatController controller = new ChatController(svc);
        UUID chatId = UUID.randomUUID();
        var msgs = new ChatMessagesResponse(List.of(
                new ChatMessageResponse(UUID.randomUUID(), chatId, "alice", UUID.randomUUID(), "a", Instant.now())
        ));
        when(svc.getMessages(chatId)).thenReturn(msgs);

        var resp = controller.getMessages(chatId);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().messages().size());
    }

    @Test
    void quickOverview_success_returns200AndItems() {
        ChatService svc = mock(ChatService.class);
        ChatController controller = new ChatController(svc);
        UUID userId = UUID.randomUUID();
        var items = List.of(
                new ChatQuickOverviewItemResponse(UUID.randomUUID(), "bob", "hey", Instant.now())
        );
        when(svc.getQuickOverview(userId)).thenReturn(items);

        var resp = controller.getQuickOverview(userId);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().size());
        assertEquals("bob", resp.getBody().get(0).otherUsername());
    }
}
