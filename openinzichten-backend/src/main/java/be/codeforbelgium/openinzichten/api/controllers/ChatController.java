package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.request.SendMessageRequest;
import be.codeforbelgium.openinzichten.api.response.ChatMessageResponse;
import be.codeforbelgium.openinzichten.api.response.ChatMessagesResponse;
import be.codeforbelgium.openinzichten.api.response.ChatQuickOverviewItemResponse;
import be.codeforbelgium.openinzichten.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<ChatMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest req,
                                                           Principal principal) {
        String username = principal.getName();
        return ResponseEntity.ok(chatService.sendMessage(username, req.chatId(), req.message()));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatMessagesResponse> getMessages(@PathVariable("chatId") UUID chatId) {
        return ResponseEntity.ok(chatService.getMessages(chatId));
    }

    @GetMapping("/quickoverview/{userId}")
    public ResponseEntity<java.util.List<ChatQuickOverviewItemResponse>> getQuickOverview(
            @PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(chatService.getQuickOverview(userId));
    }
}
