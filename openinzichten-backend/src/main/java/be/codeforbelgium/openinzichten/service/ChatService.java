package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.ChatMessageResponse;
import be.codeforbelgium.openinzichten.api.response.ChatMessagesResponse;
import be.codeforbelgium.openinzichten.api.response.ChatQuickOverviewItemResponse;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.ChatMessage;
import be.codeforbelgium.openinzichten.domain.Connection;
import be.codeforbelgium.openinzichten.domain.ConnectionState;
import be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException;
import be.codeforbelgium.openinzichten.exceptions.ChatAccessDeniedException;
import be.codeforbelgium.openinzichten.exceptions.ChatNotFoundException;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.ChatMessageRepository;
import be.codeforbelgium.openinzichten.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatMessageRepository chatMessageRepository;
    private final ConnectionRepository connectionRepository;
    private final AccountRepository accountRepository;
    private final MessageEncryptionService encryptionService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageResponse sendMessage(String senderUsername, UUID chatId, String content) {
        Account sender = accountRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for username: " + senderUsername));

        var connection = connectionRepository.findByChatId(chatId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));

        boolean isParticipant = (connection.getAccountA() != null
                && connection.getAccountA().getId().equals(sender.getId())) ||
                (connection.getAccountB() != null && connection.getAccountB().getId().equals(sender.getId()));

        if (!isParticipant) {
            log.warn("sendMessage forbidden: user={} chatId={}", senderUsername, chatId);
            throw new ChatAccessDeniedException();
        }

        // Encrypt message content before saving to DB
        ChatMessage message = ChatMessage.builder()
                .chatId(chatId)
                .sender(sender)
                .content(encryptionService.encrypt(content))
                .createdAt(Instant.now())
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        log.info("Message sent: chatId={} messageId={} by={}", chatId, saved.getId(), senderUsername);
        // Decrypt stored content before returning
        String decrypted = encryptionService.decrypt(saved.getContent());
        saved.setContent(decrypted);
        ChatMessageResponse resp = map(saved);
        // publish to subscribed websocket clients for this chat
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + chatId.toString(), resp);
        } catch (Exception e) {
            log.warn("Failed to publish websocket message for chatId={}: {}", chatId, e.getMessage());
        }
        return resp;
    }

    public ChatMessagesResponse getMessages(UUID chatId) {
        // Ensure chat exists for clearer 404 when empty/non-existent
        connectionRepository.findByChatId(chatId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));

        List<ChatMessageResponse> items = chatMessageRepository
                .findByChatIdOrderByCreatedAtAsc(chatId)
                .stream()
                .map(m -> {
                    // decrypt content when mapping
                    String dec = encryptionService.decrypt(m.getContent());
                    m.setContent(dec);
                    return map(m);
                })
                .toList();
        log.debug("Fetched {} messages for chatId={}", items.size(), chatId);
        return new ChatMessagesResponse(items);
    }

    public List<ChatQuickOverviewItemResponse> getQuickOverview(UUID userId) {
        Account viewer = accountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for id: " + userId));

        List<Connection> all = connectionRepository.findByAccountA_IdOrAccountB_Id(userId, userId);

        return all.stream()
                .filter(c -> c.getStatus() == ConnectionState.ACCEPTED)
                .map(c -> {
                    Account other = viewer.getId().equals(c.getAccountA().getId()) ? c.getAccountB() : c.getAccountA();
                    ChatMessage last = chatMessageRepository.findFirstByChatIdOrderByCreatedAtDesc(c.getChatId());
                    if (last != null) {
                        last.setContent(encryptionService.decrypt(last.getContent()));
                    }
                    return new ChatQuickOverviewItemResponse(
                            c.getChatId(),
                            other.getUsername(),
                            last != null ? last.getContent() : null,
                            last != null ? last.getCreatedAt() : null);
                })
                .sorted((a, b) -> {
                    Instant at = a.lastMessageTime();
                    Instant bt = b.lastMessageTime();
                    if (at == null && bt == null)
                        return 0;
                    if (at == null)
                        return 1;
                    if (bt == null)
                        return -1;
                    return bt.compareTo(at);
                })
                .toList();
    }

    private ChatMessageResponse map(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(),
                m.getChatId(),
                m.getSender().getUsername(),
                m.getSender().getId(),
                m.getContent(),
                m.getCreatedAt());
    }
}
