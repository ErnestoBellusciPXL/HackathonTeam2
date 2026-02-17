package be.codeforbelgium.openinzichten.api.response;

import java.time.Instant;
import java.util.UUID;

public record ChatQuickOverviewItemResponse(
        UUID chatId,
        String otherUsername,
        String lastMessage,
        Instant lastMessageTime) {
}
