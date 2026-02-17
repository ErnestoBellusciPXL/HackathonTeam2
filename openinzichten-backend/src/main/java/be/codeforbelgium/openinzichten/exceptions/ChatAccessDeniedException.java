package be.codeforbelgium.openinzichten.exceptions;

public class ChatAccessDeniedException extends RuntimeException {
    public ChatAccessDeniedException() {
        super("You are not a participant of this chat");
    }
}
