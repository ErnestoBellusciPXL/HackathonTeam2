package be.codeforbelgium.openinzichten.exceptions;

import java.util.UUID;

public class ChatNotFoundException extends RuntimeException {
    public ChatNotFoundException(UUID chatId) {
        super("Chat not found: " + chatId);
    }
}
