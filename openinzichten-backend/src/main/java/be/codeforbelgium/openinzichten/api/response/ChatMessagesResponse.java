package be.codeforbelgium.openinzichten.api.response;

import java.util.List;

public record ChatMessagesResponse(
        List<ChatMessageResponse> messages) {
}
