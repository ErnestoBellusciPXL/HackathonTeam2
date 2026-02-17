package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.domain.*;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.ChatMessageRepository;
import be.codeforbelgium.openinzichten.repository.ConditionRepository;
import be.codeforbelgium.openinzichten.repository.ConnectionRepository;
import be.codeforbelgium.openinzichten.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AccountServiceTests {

    private AccountRepository repo;
    private ConnectionRepository connectionRepository;
    private ChatMessageRepository chatMessageRepository;
    private TicketRepository ticketRepository;
    private ConditionRepository conditionRepository;
    private PasswordEncoder encoder;
    private AccountService svc;

    @BeforeEach
    void setUp() {
        repo = mock(AccountRepository.class);
        connectionRepository = mock(ConnectionRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        ticketRepository = mock(TicketRepository.class);
        conditionRepository = mock(ConditionRepository.class);
        encoder = new BCryptPasswordEncoder();
        svc = new AccountService(encoder, repo, connectionRepository, chatMessageRepository, ticketRepository, conditionRepository);
    }

    @Test
    void deleteAccount_success_when_password_matches() {
        UUID id = UUID.randomUUID();
        Account a = Account.builder()
                .id(id)
                .username("u")
                .email("e")
                .password(encoder.encode("pw"))
                .roles(List.of("User"))
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(a));
        when(connectionRepository.findByAccountA_IdOrAccountB_Id(id, id)).thenReturn(List.of());
        Ticket reporterTicket = new Ticket();
        reporterTicket.setId(UUID.randomUUID());
        reporterTicket.setReporter(a);

        Story ownedStory = Story.builder()
                .id(UUID.randomUUID())
                .title("title")
                .content("x".repeat(30))
                .owner(a)
                .build();
        Ticket storyTicket = new Ticket();
        storyTicket.setId(UUID.randomUUID());
        storyTicket.setReportee(a);
        storyTicket.setStory(ownedStory);
        storyTicket.setState(TicketState.OPEN);

        Ticket reporteeTicket = new Ticket();
        reporteeTicket.setId(UUID.randomUUID());
        reporteeTicket.setReportee(a);
        reporteeTicket.setState(TicketState.OPEN);

        when(ticketRepository.findAllByReporterId(id)).thenReturn(List.of(reporterTicket));
        when(ticketRepository.findAllByStoryOwnerId(id)).thenReturn(List.of(storyTicket));
        when(ticketRepository.findAllByReporteeId(id)).thenReturn(List.of(reporteeTicket));
        when(ticketRepository.saveAll(anyCollection())).thenAnswer(inv -> new ArrayList<>(inv.getArgument(0)));

        boolean result = svc.deleteAccountById(id, "pw");

        assertThat(result).isTrue();
        assertThat(reporterTicket.getReporter()).isNull();
        assertThat(storyTicket.getReportee()).isNull();
        assertThat(storyTicket.getStory()).isNull();
        assertThat(storyTicket.getState()).isEqualTo(TicketState.CLOSED);
        assertThat(reporteeTicket.getReportee()).isNull();
        var expectedTickets = new HashSet<>(List.of(reporterTicket, storyTicket, reporteeTicket));
        verify(chatMessageRepository).deleteBySenderId(id);
        verify(ticketRepository).saveAllAndFlush(argThat(iterable -> {
            List<Ticket> saved = new ArrayList<>();
            iterable.forEach(saved::add);
            return saved.containsAll(expectedTickets) && expectedTickets.containsAll(saved);
        }));
        verify(repo).delete(a);
    }

    @Test
    void deleteAccount_fails_when_password_does_not_match() {
        UUID id = UUID.randomUUID();
        Account a = Account.builder()
                .id(id)
                .username("u")
                .email("e")
                .password(encoder.encode("pw"))
                .roles(List.of("User"))
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(a));

        boolean result = svc.deleteAccountById(id, "wrong");

        assertThat(result).isFalse();
        verify(repo, never()).delete(any());
        verifyNoInteractions(chatMessageRepository, ticketRepository, connectionRepository);
    }

    @Test
    void deleteAccount_fails_when_account_missing() {
        UUID id = UUID.randomUUID();

        when(repo.findById(id)).thenReturn(Optional.empty());

        boolean result = svc.deleteAccountById(id, "pw");

        assertThat(result).isFalse();
        verify(repo, never()).delete(any());
        verifyNoInteractions(chatMessageRepository, ticketRepository, connectionRepository);
    }

    @Test
    void deleteAccount_fails_when_password_is_null() {
        UUID id = UUID.randomUUID();
        Account a = Account.builder()
                .id(id)
                .username("u")
                .email("e")
                .password(encoder.encode("pw"))
                .roles(List.of("User"))
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(a));

        boolean result = svc.deleteAccountById(id, null);

        assertThat(result).isFalse();
        verify(repo, never()).delete(any());
        verifyNoInteractions(chatMessageRepository, ticketRepository, connectionRepository);
    }

    @Test
    void deleteAccount_deletes_connections_and_does_not_save_when_no_tickets() {
        UUID id = UUID.randomUUID();
        Account a = Account.builder()
                .id(id)
                .username("u")
                .email("e")
                .password(encoder.encode("pw"))
                .roles(List.of("User"))
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(a));
        Connection conn = mock(Connection.class);
        when(connectionRepository.findByAccountA_IdOrAccountB_Id(id, id)).thenReturn(List.of(conn));
        when(ticketRepository.findAllByReporterId(id)).thenReturn(List.of());
        when(ticketRepository.findAllByStoryOwnerId(id)).thenReturn(List.of());
        when(ticketRepository.findAllByReporteeId(id)).thenReturn(List.of());

        boolean result = svc.deleteAccountById(id, "pw");

        assertThat(result).isTrue();
        verify(chatMessageRepository).deleteBySenderId(id);
        verify(connectionRepository).deleteAll(List.of(conn));
        verify(ticketRepository, never()).saveAllAndFlush(anyCollection());
        verify(repo).delete(a);
    }

    @Test
    void getAccountById_success() {
        UUID userId = UUID.randomUUID();
        Condition condition1 = new Condition();
        condition1.setName("Diabetes");
        Condition condition2 = new Condition();
        condition2.setName("Asthma");

        Account account = Account.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .zipcode("1000")
                .hasCondition(true)
                .conditions(new HashSet<>(Set.of(condition1, condition2)))
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));

        var result = svc.getAccountById(userId);

        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.zipcode()).isEqualTo("1000");
        assertThat(result.hasCondition()).isTrue();
        assertThat(result.conditions()).hasSize(2);
        assertThat(result.conditions()).contains("Asthma", "Diabetes");
    }

    @Test
    void getAccountById_account_not_found() {
        UUID userId = UUID.randomUUID();
        when(repo.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> svc.getAccountById(userId));
        assertThat(ex.getMessage()).isEqualTo("Account not found");
    }

    @Test
    void updateAccount_success() {
        UUID userId = UUID.randomUUID();
        Condition existingCondition = new Condition();
        existingCondition.setName("OldCondition");

        Account account = Account.builder()
                .id(userId)
                .username("olduser")
                .email("old@example.com")
                .zipcode("1000")
                .hasCondition(true)
                .conditions(new HashSet<>(Set.of(existingCondition)))
                .build();

        Condition newCondition = new Condition();
        newCondition.setName("NewCondition");

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(repo.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(conditionRepository.findByName("NewCondition")).thenReturn(Optional.of(newCondition));
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("newuser");
        req.setEmail("new@example.com");
        req.setConditions(Set.of("NewCondition"));

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().username()).isEqualTo("newuser");
        assertThat(result.account().email()).isEqualTo("new@example.com");
        assertThat(result.account().conditions()).hasSize(1);
        assertThat(result.account().conditions()).contains("NewCondition");
        assertThat(result.usernameChanged()).isTrue();
        assertThat(result.emailChanged()).isTrue();
        verify(repo).save(account);
    }

    @Test
    void updateAccount_updates_hasCondition_flag() {
        UUID userId = UUID.randomUUID();
        Condition existingCondition = new Condition();
        existingCondition.setName("ExistingCondition");

        Account account = Account.builder()
                .id(userId)
                .username("user")
                .email("user@example.com")
                .zipcode("1000")
                .hasCondition(true)
                .conditions(new HashSet<>(Set.of(existingCondition)))
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByUsername("user")).thenReturn(Optional.of(account));
        when(repo.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setHasCondition(false);
        req.setConditions(Set.of());

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().hasCondition()).isFalse();
        assertThat(result.account().conditions()).isEmpty();
    }

    @Test
    void updateAccount_updates_zipcode_without_clearing_conditions() {
        UUID userId = UUID.randomUUID();
        Condition existingCondition = new Condition();
        existingCondition.setName("ExistingCondition");

        Account account = Account.builder()
                .id(userId)
                .username("user")
                .email("user@example.com")
                .zipcode("1000")
                .hasCondition(true)
                .conditions(new HashSet<>(Set.of(existingCondition)))
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setZipcode("2000");

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().zipcode()).isEqualTo("2000");
        assertThat(result.account().conditions()).containsExactly("ExistingCondition");
        assertThat(result.account().hasCondition()).isTrue();
        assertThat(result.usernameChanged()).isFalse();
        assertThat(result.emailChanged()).isFalse();
    }

    @Test
    void updateAccount_account_not_found() {
        UUID userId = UUID.randomUUID();
        when(repo.findById(userId)).thenReturn(Optional.empty());

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> svc.updateAccount(userId, req));
        assertThat(ex.getMessage()).isEqualTo("Account not found");
    }

    @Test
    void updateAccount_username_already_taken() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("myuser")
                .email("my@example.com")
                .conditions(new HashSet<>())
                .build();

        Account otherAccount = Account.builder()
                .id(UUID.randomUUID())
                .username("takenuser")
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByUsername("takenuser")).thenReturn(Optional.of(otherAccount));
        when(repo.findByEmail("my@example.com")).thenReturn(Optional.of(account));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("takenuser");
        req.setEmail("my@example.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> svc.updateAccount(userId, req));
        assertThat(ex.getMessage()).isEqualTo("Username already taken");
    }

    @Test
    void updateAccount_email_already_in_use() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("myuser")
                .email("my@example.com")
                .conditions(new HashSet<>())
                .build();

        Account otherAccount = Account.builder()
                .id(UUID.randomUUID())
                .email("taken@example.com")
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByUsername("myuser")).thenReturn(Optional.of(account));
        when(repo.findByEmail("taken@example.com")).thenReturn(Optional.of(otherAccount));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("myuser");
        req.setEmail("taken@example.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> svc.updateAccount(userId, req));
        assertThat(ex.getMessage()).isEqualTo("Email already in use");
    }

    @Test
    void updateAccount_same_username_and_email_allowed() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("myuser")
                .email("my@example.com")
                .conditions(new HashSet<>())
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByUsername("myuser")).thenReturn(Optional.of(account));
        when(repo.findByEmail("my@example.com")).thenReturn(Optional.of(account));
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("myuser");
        req.setEmail("my@example.com");

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().username()).isEqualTo("myuser");
        assertThat(result.account().email()).isEqualTo("my@example.com");
        assertThat(result.usernameChanged()).isFalse();
        assertThat(result.emailChanged()).isFalse();
    }

    @Test
    void updateAccount_blank_email_and_username_not_updated() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("originaluser")
                .email("original@example.com")
                .conditions(new HashSet<>())
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("");
        req.setEmail("   ");

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().username()).isEqualTo("originaluser");
        assertThat(result.account().email()).isEqualTo("original@example.com");
        assertThat(result.usernameChanged()).isFalse();
        assertThat(result.emailChanged()).isFalse();
        verify(repo, never()).findByUsername(anyString());
        verify(repo, never()).findByEmail(anyString());
    }

    @Test
    void updateAccount_null_conditions() {
        UUID userId = UUID.randomUUID();
        Condition existingCondition = new Condition();
        existingCondition.setName("ExistingCondition");

        Account account = Account.builder()
                .id(userId)
                .username("user")
                .email("user@example.com")
                .zipcode("1000")
                .hasCondition(true)
                .conditions(new HashSet<>(Set.of(existingCondition)))
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByUsername("user")).thenReturn(Optional.of(account));
        when(repo.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("user");
        req.setEmail("user@example.com");
        req.setConditions(null);

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().conditions()).containsExactly("ExistingCondition");
        assertThat(result.account().hasCondition()).isTrue();
        assertThat(result.account().zipcode()).isEqualTo("1000");
        assertThat(result.usernameChanged()).isFalse();
        assertThat(result.emailChanged()).isFalse();
    }

    @Test
    void updateAccount_condition_not_found_in_repository() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("user")
                .email("user@example.com")
                .conditions(new HashSet<>())
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByUsername("user")).thenReturn(Optional.of(account));
        when(repo.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(conditionRepository.findByName("NonExistent")).thenReturn(Optional.empty());
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("user");
        req.setEmail("user@example.com");
        req.setConditions(Set.of("NonExistent"));

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().conditions()).isEmpty();
    }

    @Test
    void updateAccount_null_username_with_valid_email() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("originaluser")
                .email("original@example.com")
                .conditions(new HashSet<>())
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername(null);
        req.setEmail("new@example.com");

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().username()).isEqualTo("originaluser");
        assertThat(result.account().email()).isEqualTo("new@example.com");
        assertThat(result.usernameChanged()).isFalse();
        assertThat(result.emailChanged()).isTrue();
        verify(repo, never()).findByUsername(anyString());
    }

    @Test
    void updateAccount_valid_username_with_null_email() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("originaluser")
                .email("original@example.com")
                .conditions(new HashSet<>())
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("newuser");
        req.setEmail(null);

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().username()).isEqualTo("newuser");
        assertThat(result.account().email()).isEqualTo("original@example.com");
        assertThat(result.usernameChanged()).isTrue();
        assertThat(result.emailChanged()).isFalse();
        verify(repo, never()).findByEmail(anyString());
    }

    @Test
    void updateAccount_both_null_values() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("originaluser")
                .email("original@example.com")
                .conditions(new HashSet<>())
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername(null);
        req.setEmail(null);

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().username()).isEqualTo("originaluser");
        assertThat(result.account().email()).isEqualTo("original@example.com");
        assertThat(result.usernameChanged()).isFalse();
        assertThat(result.emailChanged()).isFalse();
        verify(repo, never()).findByUsername(anyString());
        verify(repo, never()).findByEmail(anyString());
    }

    @Test
    void updateAccount_blank_username_with_valid_email() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("originaluser")
                .email("original@example.com")
                .conditions(new HashSet<>())
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("");
        req.setEmail("new@example.com");

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().username()).isEqualTo("originaluser");
        assertThat(result.account().email()).isEqualTo("new@example.com");
        assertThat(result.usernameChanged()).isFalse();
        assertThat(result.emailChanged()).isTrue();
        verify(repo, never()).findByUsername(anyString());
    }

    @Test
    void updateAccount_valid_username_with_blank_email() {
        UUID userId = UUID.randomUUID();
        Account account = Account.builder()
                .id(userId)
                .username("originaluser")
                .email("original@example.com")
                .conditions(new HashSet<>())
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(account));
        when(repo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest req =
                new be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest();
        req.setUsername("newuser");
        req.setEmail("   ");

        var result = svc.updateAccount(userId, req);

        assertThat(result.account().username()).isEqualTo("newuser");
        assertThat(result.account().email()).isEqualTo("original@example.com");
        assertThat(result.usernameChanged()).isTrue();
        assertThat(result.emailChanged()).isFalse();
        verify(repo, never()).findByEmail(anyString());
    }
}

