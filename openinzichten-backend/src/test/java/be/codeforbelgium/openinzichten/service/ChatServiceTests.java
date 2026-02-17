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
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTests {

    private Account mkAccount(UUID id, String username) {
        return Account.builder()
                .id(id)
                .username(username)
                .email(username + "@mail.test")
                .password("pw")
                .roles(List.of("USER"))
                .hasCondition(false)
                .build();
    }

    private Connection mkConn(Account a, Account b, ConnectionState state) {
        return Connection.builder().accountA(a).accountB(b).status(state).build();
    }

    @Test
    void sendMessage_success_savesAndMaps() {
        ChatMessageRepository msgRepo = mock(ChatMessageRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MessageEncryptionService enc = mock(MessageEncryptionService.class);
        SimpMessagingTemplate simp = mock(SimpMessagingTemplate.class);
        when(enc.encrypt(anyString())).thenAnswer(i -> i.getArgument(0));
        when(enc.decrypt(anyString())).thenAnswer(i -> i.getArgument(0));
        ChatService svc = new ChatService(msgRepo, connRepo, accRepo, enc, simp);

        UUID chatId = UUID.randomUUID();
        Account sender = mkAccount(UUID.randomUUID(), "alice");
        Account other = mkAccount(UUID.randomUUID(), "bob");
        when(accRepo.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(connRepo.findByChatId(chatId)).thenReturn(Optional.of(mkConn(sender, other, ConnectionState.ACCEPTED)));

        ChatMessage saved = ChatMessage.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .sender(sender)
                .content("hello")
                .createdAt(Instant.now())
                .build();
        when(msgRepo.save(any(ChatMessage.class))).thenReturn(saved);

        ChatMessageResponse resp = svc.sendMessage("alice", chatId, "hello");
        assertEquals(saved.getId(), resp.id());
        assertEquals("alice", resp.senderUsername());
        assertEquals("hello", resp.content());
    }

    @Test
    void sendMessage_senderNotFound_throws() {
        ChatMessageRepository msgRepo = mock(ChatMessageRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MessageEncryptionService enc = mock(MessageEncryptionService.class);
        SimpMessagingTemplate simp = mock(SimpMessagingTemplate.class);
        when(accRepo.findByUsername("nouser")).thenReturn(Optional.empty());
        ChatService svc = new ChatService(msgRepo, connRepo, accRepo, enc, simp);
        UUID id = UUID.randomUUID();
        assertThrows(AccountNotFoundException.class, () -> svc.sendMessage("nouser", id, "x"));
    }

    @Test
    void sendMessage_chatNotFound_throws() {
        ChatMessageRepository msgRepo = mock(ChatMessageRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MessageEncryptionService enc = mock(MessageEncryptionService.class);
        SimpMessagingTemplate simp = mock(SimpMessagingTemplate.class);
        UUID chatId = UUID.randomUUID();
        when(accRepo.findByUsername("alice")).thenReturn(Optional.of(mkAccount(UUID.randomUUID(), "alice")));
        when(connRepo.findByChatId(chatId)).thenReturn(Optional.empty());
        ChatService svc = new ChatService(msgRepo, connRepo, accRepo, enc, simp);
        assertThrows(ChatNotFoundException.class, () -> svc.sendMessage("alice", chatId, "x"));
    }

    @Test
    void sendMessage_notParticipant_throwsAccessDenied() {
        ChatMessageRepository msgRepo = mock(ChatMessageRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MessageEncryptionService enc = mock(MessageEncryptionService.class);
        SimpMessagingTemplate simp = mock(SimpMessagingTemplate.class);
        UUID chatId = UUID.randomUUID();
        Account sender = mkAccount(UUID.randomUUID(), "alice");
        Account a = mkAccount(UUID.randomUUID(), "charlie");
        Account b = mkAccount(UUID.randomUUID(), "dana");
        when(accRepo.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(connRepo.findByChatId(chatId)).thenReturn(Optional.of(mkConn(a, b, ConnectionState.ACCEPTED)));
        ChatService svc = new ChatService(msgRepo, connRepo, accRepo, enc, simp);
        assertThrows(ChatAccessDeniedException.class, () -> svc.sendMessage("alice", chatId, "x"));
    }

    @Test
    void getMessages_success_returnsOrderedMappedList() {
        ChatMessageRepository msgRepo = mock(ChatMessageRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MessageEncryptionService enc = mock(MessageEncryptionService.class);
        SimpMessagingTemplate simp = mock(SimpMessagingTemplate.class);
        when(enc.decrypt(anyString())).thenAnswer(i -> i.getArgument(0));
        ChatService svc = new ChatService(msgRepo, connRepo, accRepo, enc, simp);

        UUID chatId = UUID.randomUUID();
        when(connRepo.findByChatId(chatId)).thenReturn(Optional.of(mkConn(mkAccount(UUID.randomUUID(), "a"), mkAccount(UUID.randomUUID(), "b"), ConnectionState.ACCEPTED)));

        Account sender = mkAccount(UUID.randomUUID(), "alice");
        ChatMessage m1 = ChatMessage.builder().id(UUID.randomUUID()).chatId(chatId).sender(sender).content("1").createdAt(Instant.now().minusSeconds(10)).build();
        ChatMessage m2 = ChatMessage.builder().id(UUID.randomUUID()).chatId(chatId).sender(sender).content("2").createdAt(Instant.now()).build();
        when(msgRepo.findByChatIdOrderByCreatedAtAsc(chatId)).thenReturn(List.of(m1, m2));

        ChatMessagesResponse resp = svc.getMessages(chatId);
        assertEquals(2, resp.messages().size());
        assertEquals("1", resp.messages().get(0).content());
        assertEquals("2", resp.messages().get(1).content());
    }

    @Test
    void getMessages_chatNotFound_throws() {
        ChatMessageRepository msgRepo = mock(ChatMessageRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MessageEncryptionService enc = mock(MessageEncryptionService.class);
        SimpMessagingTemplate simp = mock(SimpMessagingTemplate.class);
        ChatService svc = new ChatService(msgRepo, connRepo, accRepo, enc, simp);
        UUID chatId = UUID.randomUUID();
        when(connRepo.findByChatId(chatId)).thenReturn(Optional.empty());
        assertThrows(ChatNotFoundException.class, () -> svc.getMessages(chatId));
    }

    @Test
    void getQuickOverview_success_filtersAcceptedAndSortsByLastMessageDesc() {
        ChatMessageRepository msgRepo = mock(ChatMessageRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MessageEncryptionService enc = mock(MessageEncryptionService.class);
        SimpMessagingTemplate simp = mock(SimpMessagingTemplate.class);
        when(enc.decrypt(anyString())).thenAnswer(i -> i.getArgument(0));
        ChatService svc = new ChatService(msgRepo, connRepo, accRepo, enc, simp);

        UUID viewerId = UUID.randomUUID();
        Account viewer = mkAccount(viewerId, "viewer");
        when(accRepo.findById(viewerId)).thenReturn(Optional.of(viewer));

        Account a = mkAccount(UUID.randomUUID(), "alice");
        Account b = mkAccount(UUID.randomUUID(), "bob");
        Account c = mkAccount(UUID.randomUUID(), "carol");

        Connection c1 = mkConn(viewer, a, ConnectionState.ACCEPTED);
        Connection c2 = mkConn(b, viewer, ConnectionState.ACCEPTED);
        Connection c3 = mkConn(viewer, c, ConnectionState.DECLINED); // should be filtered out

        UUID chat1 = UUID.randomUUID();
        c1.setChatId(chat1);
        UUID chat2 = UUID.randomUUID();
        c2.setChatId(chat2);
        UUID chat3 = UUID.randomUUID();
        c3.setChatId(chat3);

        when(connRepo.findByAccountA_IdOrAccountB_Id(viewerId, viewerId)).thenReturn(List.of(c1, c2, c3));

        ChatMessage last2 = ChatMessage.builder().id(UUID.randomUUID()).chatId(chat2).sender(viewer).content("z").createdAt(Instant.now()).build();
        ChatMessage last1 = ChatMessage.builder().id(UUID.randomUUID()).chatId(chat1).sender(viewer).content("a").createdAt(Instant.now().minusSeconds(60)).build();
        when(msgRepo.findFirstByChatIdOrderByCreatedAtDesc(chat1)).thenReturn(last1);
        when(msgRepo.findFirstByChatIdOrderByCreatedAtDesc(chat2)).thenReturn(last2);
        when(msgRepo.findFirstByChatIdOrderByCreatedAtDesc(chat3)).thenReturn(null);

        List<ChatQuickOverviewItemResponse> items = svc.getQuickOverview(viewerId);
        assertEquals(2, items.size());
        // chat2 has later last message than chat1
        assertEquals(chat2, items.get(0).chatId());
        assertEquals("bob", items.get(0).otherUsername());
        assertEquals("z", items.get(0).lastMessage());
        assertEquals("a", items.get(1).lastMessage());
    }

    @Test
    void getQuickOverview_viewerNotFound_throws() {
        ChatMessageRepository msgRepo = mock(ChatMessageRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        MessageEncryptionService enc = mock(MessageEncryptionService.class);
        SimpMessagingTemplate simp = mock(SimpMessagingTemplate.class);
        ChatService svc = new ChatService(msgRepo, connRepo, accRepo, enc, simp);
        when(accRepo.findById(any())).thenReturn(Optional.empty());
        UUID viewerId = UUID.randomUUID();
        assertThrows(AccountNotFoundException.class, () -> svc.getQuickOverview(viewerId));
    }
}
