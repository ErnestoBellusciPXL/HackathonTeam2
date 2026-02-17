package be.codeforbelgium.openinzichten.config;

import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDataLoaderTests {
    private final String adminEmail = "admin@openinzicht.be";
    private final String adminPassword = "SecureP4assword!";
    private final String adminUsername = "Administrator";


    @Test
    void runner_skips_when_admin_exists() throws Exception {
        AccountRepository repo = mock(AccountRepository.class);
        when(repo.findByEmail(adminEmail)).thenReturn(Optional.of(new Account()));

        UserDataLoader loader = new UserDataLoader(adminEmail, adminPassword);
        CommandLineRunner runner = loader.commandLineRunner(repo);

        runner.run();

        verify(repo, never()).save(any());
    }

    @Test
    void runner_creates_admin_with_encoded_password_and_roles() throws Exception {
        AccountRepository repo = mock(AccountRepository.class);
        when(repo.findByEmail(adminEmail)).thenReturn(Optional.empty());
        when(repo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDataLoader loader = new UserDataLoader(adminEmail, adminPassword);
        CommandLineRunner runner = loader.commandLineRunner(repo);

        runner.run();

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(repo).save(captor.capture());
        Account saved = captor.getValue();

        assertEquals(adminUsername, saved.getUsername());
        assertEquals(adminEmail, saved.getEmail());
        assertNotNull(saved.getRoles());
        assertTrue(saved.getRoles().containsAll(List.of("Admin", "User")));

        // Ensure password is encoded and matches the configured literal
        assertNotNull(saved.getPassword());
        assertNotEquals(adminPassword, saved.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches(adminPassword, saved.getPassword()));
    }
}
