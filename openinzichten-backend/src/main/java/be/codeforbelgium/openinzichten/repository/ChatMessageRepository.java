package be.codeforbelgium.openinzichten.repository;

import be.codeforbelgium.openinzichten.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(UUID chatId);

    ChatMessage findFirstByChatIdOrderByCreatedAtDesc(UUID chatId);

    void deleteBySenderId(UUID senderId);
}
