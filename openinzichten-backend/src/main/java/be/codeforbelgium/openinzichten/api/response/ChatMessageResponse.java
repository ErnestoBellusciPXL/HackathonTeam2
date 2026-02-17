package be.codeforbelgium.openinzichten.api.response;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID chatId,
        String senderUsername,
        UUID senderId,
        String content,
        Instant createdAt) {
}
