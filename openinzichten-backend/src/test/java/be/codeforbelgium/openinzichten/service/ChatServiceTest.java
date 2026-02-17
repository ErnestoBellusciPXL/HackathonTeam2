package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.ChatMessageResponse;
import be.codeforbelgium.openinzichten.api.response.ChatMessagesResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private ChatMessageRepository chatMessageRepository;
    private ConnectionRepository connectionRepository;
    private AccountRepository accountRepository;
    private MessageEncryptionService encryptionService;
    private SimpMessagingTemplate messagingTemplate;
    private ChatService svc;

    @BeforeEach
    void setUp() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        connectionRepository = mock(ConnectionRepository.class);
        accountRepository = mock(AccountRepository.class);
        encryptionService = mock(MessageEncryptionService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        svc = new ChatService(chatMessageRepository, connectionRepository, accountRepository, encryptionService, messagingTemplate);
    }

    @Test
    void sendMessage_happyPath_savesAndPublishes() {
        UUID chatId = UUID.randomUUID();
        Account sender = Account.builder().id(UUID.randomUUID()).username("alice").email("a@b").password("x").roles(List.of("USER")).build();

        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

        Connection conn = Connection.builder().chatId(chatId).accountA(sender).accountB(Account.builder().id(UUID.randomUUID()).username("bob").email("b@c").password("p").roles(List.of("USER")).build()).status(ConnectionState.ACCEPTED).build();
        when(connectionRepository.findByChatId(chatId)).thenReturn(Optional.of(conn));

        when(encryptionService.encrypt(any())).thenAnswer(i -> "enc:" + i.getArgument(0));
        when(encryptionService.decrypt(any())).thenAnswer(i -> {
            String s = (String) i.getArgument(0);
            return s != null && s.startsWith("enc:") ? s.substring(4) : s;
        });

        ArgumentCaptor<ChatMessage> cap = ArgumentCaptor.forClass(ChatMessage.class);
        when(chatMessageRepository.save(cap.capture())).thenAnswer(i -> {
            ChatMessage m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        ChatMessageResponse resp = svc.sendMessage("alice", chatId, "hey");
        assertNotNull(resp);
        assertEquals("alice", resp.senderUsername());
        assertEquals("hey", resp.content());

        verify(chatMessageRepository).save(any());
    }

    @Test
    void sendMessage_publishFailure_logsButReturns() {
        UUID chatId = UUID.randomUUID();
        Account sender = Account.builder().id(UUID.randomUUID()).username("alice").email("a@b").password("x").roles(List.of("USER")).build();
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        Connection conn = Connection.builder().chatId(chatId).accountA(sender).accountB(Account.builder().id(UUID.randomUUID()).username("bob").email("b@c").password("p").roles(List.of("USER")).build()).status(ConnectionState.ACCEPTED).build();
        when(connectionRepository.findByChatId(chatId)).thenReturn(Optional.of(conn));
        when(encryptionService.encrypt(any())).thenReturn("enc:hey");
        when(chatMessageRepository.save(any())).thenAnswer(i -> {
            ChatMessage m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        // decrypt should return the original content (strip the "enc:" prefix)
        when(encryptionService.decrypt(any())).thenAnswer(i -> {
            String s = (String) i.getArgument(0);
            return s != null && s.startsWith("enc:") ? s.substring(4) : s;
        });

        doThrow(new RuntimeException("ws-fail")).when(messagingTemplate).convertAndSend(anyString(), Mockito.<Object>any());

        ChatMessageResponse resp = svc.sendMessage("alice", chatId, "hey");
        assertNotNull(resp);
        // despite websocket failure, method returns response
        assertEquals("hey", resp.content());
    }

    @Test
    void sendMessage_forbiddenWhenNotParticipant_throws() {
        UUID chatId = UUID.randomUUID();
        Account sender = Account.builder().id(UUID.randomUUID()).username("alice").email("a@b").password("x").roles(List.of("USER")).build();
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

        // connection with different participants
        Connection conn = Connection.builder().chatId(chatId).accountA(Account.builder().id(UUID.randomUUID()).username("john").email("j@e").password("p").roles(List.of("USER")).build()).accountB(Account.builder().id(UUID.randomUUID()).username("jane").email("j2@e").password("p").roles(List.of("USER")).build()).status(ConnectionState.ACCEPTED).build();
        when(connectionRepository.findByChatId(chatId)).thenReturn(Optional.of(conn));

        assertThrows(ChatAccessDeniedException.class, () -> svc.sendMessage("alice", chatId, "x"));
    }

    @Test
    void getMessages_nonExistingChat_throwsNotFound() {
        UUID chatId = UUID.randomUUID();
        when(connectionRepository.findByChatId(chatId)).thenReturn(Optional.empty());
        assertThrows(ChatNotFoundException.class, () -> svc.getMessages(chatId));
    }

    @Test
    void getMessages_happyPath_decryptsAll() {
        UUID chatId = UUID.randomUUID();
        when(connectionRepository.findByChatId(chatId)).thenReturn(Optional.of(Connection.builder().chatId(chatId).build()));
        ChatMessage m1 = ChatMessage.builder().id(UUID.randomUUID()).chatId(chatId).content("enc:a").createdAt(Instant.now()).sender(Account.builder().id(UUID.randomUUID()).username("a").email("e").password("p").roles(List.of("USER")).build()).build();
        ChatMessage m2 = ChatMessage.builder().id(UUID.randomUUID()).chatId(chatId).content("enc:b").createdAt(Instant.now()).sender(Account.builder().id(UUID.randomUUID()).username("b").email("e").password("p").roles(List.of("USER")).build()).build();
        when(chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId)).thenReturn(List.of(m1, m2));
        when(encryptionService.decrypt(any())).thenAnswer(i -> ((String) i.getArgument(0)).replace("enc:", ""));

        ChatMessagesResponse resp = svc.getMessages(chatId);
        assertNotNull(resp);
        assertEquals(2, resp.messages().size());
        assertEquals("a", resp.messages().get(0).content());
    }

    @Test
    void getQuickOverview_accountNotFound_throws() {
        UUID userId = UUID.randomUUID();
        when(accountRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> svc.getQuickOverview(userId));
    }

    @Test
    void getQuickOverview_returnsSortedList() {
        UUID userId = UUID.randomUUID();
        Account viewer = Account.builder().id(userId).username("viewer").email("v@e").password("p").roles(List.of("USER")).build();
        when(accountRepository.findById(userId)).thenReturn(Optional.of(viewer));

        Account other1 = Account.builder().id(UUID.randomUUID()).username("other1").email("o1@e").password("p").roles(List.of("USER")).build();
        Account other2 = Account.builder().id(UUID.randomUUID()).username("other2").email("o2@e").password("p").roles(List.of("USER")).build();

        Connection c1 = Connection.builder().chatId(UUID.randomUUID()).accountA(viewer).accountB(other1).status(ConnectionState.ACCEPTED).build();
        Connection c2 = Connection.builder().chatId(UUID.randomUUID()).accountA(other2).accountB(viewer).status(ConnectionState.ACCEPTED).build();

        when(connectionRepository.findByAccountA_IdOrAccountB_Id(userId, userId)).thenReturn(List.of(c1, c2));

        ChatMessage last1 = ChatMessage.builder().chatId(c1.getChatId()).content("enc:one").createdAt(Instant.now().minusSeconds(10)).build();
        ChatMessage last2 = ChatMessage.builder().chatId(c2.getChatId()).content("enc:two").createdAt(Instant.now()).build();
        when(chatMessageRepository.findFirstByChatIdOrderByCreatedAtDesc(c1.getChatId())).thenReturn(last1);
        when(chatMessageRepository.findFirstByChatIdOrderByCreatedAtDesc(c2.getChatId())).thenReturn(last2);

        when(encryptionService.decrypt(any())).thenAnswer(i -> ((String) i.getArgument(0)).replace("enc:", ""));

        var overview = svc.getQuickOverview(userId);
        assertEquals(2, overview.size());
        // c2 has the newest message so should be first
        assertEquals("two", overview.get(0).lastMessage());
        assertEquals("one", overview.get(1).lastMessage());
    }
}
