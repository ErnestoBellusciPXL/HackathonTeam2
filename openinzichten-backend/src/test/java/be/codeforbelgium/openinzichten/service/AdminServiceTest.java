package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.AdminAccountResponse;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.Community;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Tests")
class AdminServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AdminService adminService;

    private Account testAccount1;
    private Account testAccount2;
    private Account testAccount3;
    private Condition condition1;
    private Condition condition2;
    private Community community1;
    private Community community2;

    @BeforeEach
    void setUp() {
        // Create test conditions
        condition1 = new Condition();
        condition1.setName("Diabetes");

        condition2 = new Condition();
        condition2.setName("Hypertension");

        // Create test communities
        community1 = new Community();
        community1.setName("Brussels");

        community2 = new Community();
        community2.setName("Antwerp");

        // Account 1: User with conditions and communities
        testAccount1 = Account.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .username("user1")
                .email("user1@example.com")
                .password("password")
                .roles(List.of("User"))
                .hasCondition(true)
                .conditions(Set.of(condition1, condition2))
                .communities(Set.of(community1))
                .build();

        // Account 2: Admin user with no conditions but with communities
        testAccount2 = Account.builder()
                .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .username("admin1")
                .email("admin1@example.com")
                .password("password")
                .roles(List.of("Admin", "User"))
                .hasCondition(false)
                .conditions(Set.of())
                .communities(Set.of(community1, community2))
                .build();

        // Account 3: User with no conditions and no communities
        testAccount3 = Account.builder()
                .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .username("user3")
                .email("user3@example.com")
                .password("password")
                .roles(List.of("User"))
                .hasCondition(false)
                .conditions(Set.of())
                .communities(Set.of())
                .build();
    }

    @Test
    @DisplayName("Should return all accounts when repository has multiple accounts")
    void getAllAccounts_whenMultipleAccountsExist_shouldReturnAllAccounts() {
        // Arrange
        List<Account> accounts = Arrays.asList(testAccount1, testAccount2, testAccount3);
        Page<Account> accountPage = new PageImpl<>(accounts, PageRequest.of(0, 10), 3);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty page when no accounts exist")
    void getAllAccounts_whenNoAccountsExist_shouldReturnEmptyPage() {
        // Arrange
        Page<Account> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should correctly map account with conditions and communities")
    void getAllAccounts_shouldCorrectlyMapAccountWithConditionsAndCommunities() {
        // Arrange
        Page<Account> accountPage = new PageImpl<>(List.of(testAccount1), PageRequest.of(0, 10), 1);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        AdminAccountResponse response = result.getContent().get(0);

        assertThat(response.id()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(response.username()).isEqualTo("user1");
        assertThat(response.hasCondition()).isTrue();
        assertThat(response.conditions()).hasSize(2);
        assertThat(response.conditions()).containsExactlyInAnyOrder("Diabetes", "Hypertension");
        assertThat(response.communities()).hasSize(1);
        assertThat(response.communities()).contains("Brussels");

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should correctly map account with empty conditions and communities")
    void getAllAccounts_shouldCorrectlyMapAccountWithEmptyConditionsAndCommunities() {
        // Arrange
        Page<Account> accountPage = new PageImpl<>(List.of(testAccount3), PageRequest.of(0, 10), 1);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        AdminAccountResponse response = result.getContent().get(0);

        assertThat(response.id()).isEqualTo("33333333-3333-3333-3333-333333333333");
        assertThat(response.username()).isEqualTo("user3");
        assertThat(response.hasCondition()).isFalse();
        assertThat(response.conditions()).isEmpty();
        assertThat(response.communities()).isEmpty();

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should correctly map account with multiple communities")
    void getAllAccounts_shouldCorrectlyMapAccountWithMultipleCommunities() {
        // Arrange
        Page<Account> accountPage = new PageImpl<>(List.of(testAccount2), PageRequest.of(0, 10), 1);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        AdminAccountResponse response = result.getContent().get(0);

        assertThat(response.id()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(response.username()).isEqualTo("admin1");
        assertThat(response.hasCondition()).isFalse();
        assertThat(response.conditions()).isEmpty();
        assertThat(response.communities()).hasSize(2);
        assertThat(response.communities()).containsExactlyInAnyOrder("Brussels", "Antwerp");

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle single account correctly")
    void getAllAccounts_whenSingleAccount_shouldReturnSingleResponse() {
        // Arrange
        Page<Account> accountPage = new PageImpl<>(List.of(testAccount1), PageRequest.of(0, 10), 1);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("user1");

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should sort accounts by username in ascending order")
    void getAllAccounts_shouldSortByUsernameAscending() {
        // Arrange - note that accounts are sorted by username: admin1, user1, user3
        List<Account> orderedAccounts = Arrays.asList(testAccount2, testAccount1, testAccount3);
        Page<Account> accountPage = new PageImpl<>(orderedAccounts, PageRequest.of(0, 10), 3);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).username()).isEqualTo("admin1");
        assertThat(result.getContent().get(1).username()).isEqualTo("user1");
        assertThat(result.getContent().get(2).username()).isEqualTo("user3");

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should convert UUID to String correctly")
    void getAllAccounts_shouldConvertUuidToStringCorrectly() {
        // Arrange
        Page<Account> accountPage = new PageImpl<>(List.of(testAccount1), PageRequest.of(0, 10), 1);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isInstanceOf(String.class);
        assertThat(result.getContent().get(0).id()).isEqualTo("11111111-1111-1111-1111-111111111111");

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle account with null conditions set gracefully")
    void getAllAccounts_whenConditionsIsNull_shouldHandleGracefully() {
        // Arrange
        Account accountWithNullConditions = Account.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .roles(List.of("User"))
                .hasCondition(false)
                .conditions(null)
                .communities(Set.of())
                .build();

        Page<Account> accountPage = new PageImpl<>(List.of(accountWithNullConditions), PageRequest.of(0, 10), 1);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert - Should handle null gracefully and return empty list for conditions
        assertThat(result.getContent()).hasSize(1);
        AdminAccountResponse response = result.getContent().get(0);
        assertThat(response.conditions()).isEmpty();

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle account with null communities set gracefully")
    void getAllAccounts_whenCommunitiesIsNull_shouldHandleGracefully() {
        // Arrange
        Account accountWithNullCommunities = Account.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .roles(List.of("User"))
                .hasCondition(false)
                .conditions(Set.of())
                .communities(null)
                .build();

        Page<Account> accountPage = new PageImpl<>(List.of(accountWithNullCommunities), PageRequest.of(0, 10), 1);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        Page<AdminAccountResponse> result = adminService.getAllAccounts(0, 10);

        // Assert - Should handle null gracefully and return empty list for communities
        assertThat(result.getContent()).hasSize(1);
        AdminAccountResponse response = result.getContent().get(0);
        assertThat(response.communities()).isEmpty();

        verify(accountRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should call repository findAll method exactly once")
    void getAllAccounts_shouldCallRepositoryFindAllOnce() {
        // Arrange
        Page<Account> accountPage = new PageImpl<>(List.of(testAccount1), PageRequest.of(0, 10), 1);
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(accountPage);

        // Act
        adminService.getAllAccounts(0, 10);

        // Assert
        verify(accountRepository, times(1)).findAll(any(Pageable.class));
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    @DisplayName("Should return account detail by id")
    void getAccountById_shouldReturnDetail() {
        when(accountRepository.findById(testAccount1.getId())).thenReturn(Optional.of(testAccount1));

        var detail = adminService.getAccountById(testAccount1.getId().toString());

        assertThat(detail).isNotNull();
        assertThat(detail.username()).isEqualTo("user1");
        verify(accountRepository).findById(testAccount1.getId());
    }

    @Test
    @DisplayName("Should throw when account not found by id")
    void getAccountById_notFound_shouldThrow() {
        when(accountRepository.findById(any())).thenReturn(Optional.empty());
        String missingId = UUID.randomUUID().toString();
        assertThatThrownBy(() -> adminService.getAccountById(missingId))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("setAccountDisabled should update and send mail when disabling")
    void setAccountDisabled_disable_sendsMail() {
        MailService mail = mock(MailService.class);
        // re-create service with mail mock
        adminService = new AdminService(accountRepository, mail);

        Account acc = Account.builder().id(UUID.randomUUID()).username("u").email("e@e").disabled(false).build();
        when(accountRepository.findById(acc.getId())).thenReturn(Optional.of(acc));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = adminService.setAccountDisabled(acc.getId().toString(), true, "spamtastic");

        assertThat(res.disabled()).isTrue();
        verify(mail).sendAccountDisabled("e@e", "u", "spamtastic");
    }

    @Test
    @DisplayName("setAccountDisabled reactivation sends reactivated when previously disabled")
    void setAccountDisabled_reactivate_sendsReactivated() {
        MailService mail = mock(MailService.class);
        adminService = new AdminService(accountRepository, mail);

        Account acc = Account.builder().id(UUID.randomUUID()).username("u").email("e@e").disabled(true).build();
        when(accountRepository.findById(acc.getId())).thenReturn(Optional.of(acc));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = adminService.setAccountDisabled(acc.getId().toString(), false, null);

        assertThat(res.disabled()).isFalse();
        verify(mail).sendAccountReactivated("e@e", "u");
    }

    @Test
    @DisplayName("setAccountDisabled does not send mail when email missing")
    void setAccountDisabled_noEmail_noMailSent() {
        MailService mail = mock(MailService.class);
        adminService = new AdminService(accountRepository, mail);

        Account acc = Account.builder().id(UUID.randomUUID()).username("u").email(null).disabled(false).build();
        when(accountRepository.findById(acc.getId())).thenReturn(Optional.of(acc));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.setAccountDisabled(acc.getId().toString(), true, "r");

        verifyNoInteractions(mail);
    }
}
