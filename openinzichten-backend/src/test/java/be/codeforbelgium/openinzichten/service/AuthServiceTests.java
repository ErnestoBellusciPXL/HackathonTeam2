package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.request.CompleteRegistrationRequest;
import be.codeforbelgium.openinzichten.api.request.LoginRequest;
import be.codeforbelgium.openinzichten.api.request.RegisterRequest;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException;
import be.codeforbelgium.openinzichten.exceptions.EmailTakenException;
import be.codeforbelgium.openinzichten.exceptions.InvalidCredentialsException;
import be.codeforbelgium.openinzichten.exceptions.UsernameTakenException;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.ConditionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTests {

    @Test
    void createAccount_success_and_username_taken() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        RegisterRequest req = new RegisterRequest("bob", "b@example.com", "Password?123");

        when(arepo.findByUsername("bob")).thenReturn(Optional.empty());
        when(arepo.findByEmail("b@example.com")).thenReturn(Optional.empty());

        Account saved = Account.builder().username("bob").email("b@example.com").build();
        when(arepo.save(any(Account.class))).thenReturn(saved);

        Account r = svc.createAccount(req);
        assertEquals("bob", r.getUsername());

        // username taken
        when(arepo.findByUsername("bob")).thenReturn(Optional.of(saved));
        assertThrows(UsernameTakenException.class, () -> svc.createAccount(req));
    }

    @Test
    void createAccount_password_encoded_and_email_taken() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        RegisterRequest req = new RegisterRequest("alice", "a@example.com", "Password?123");

        when(arepo.findByUsername("alice")).thenReturn(Optional.empty());
        when(arepo.findByEmail("a@example.com")).thenReturn(Optional.empty());

        // let save return the saved entity (preserve encoded password)
        when(arepo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account created = svc.createAccount(req);
        assertEquals("alice", created.getUsername());
        assertNotNull(created.getPassword());
        assertNotEquals("s3cret", created.getPassword());
        assertTrue(created.getRoles().contains("User"));

        // email taken branch
        when(arepo.findByUsername("alice")).thenReturn(Optional.empty());
        when(arepo.findByEmail("a@example.com")).thenReturn(Optional.of(created));
        assertThrows(EmailTakenException.class, () -> svc.createAccount(req));
    }

    @Test
    void authenticate_account_not_found_and_completeRegistration_account_not_found() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        // authenticate when no account found - throws InvalidCredentialsException for security
        when(arepo.findByEmail("missing@x")).thenReturn(Optional.empty());
        LoginRequest lr = new LoginRequest("missing@x", "pw", false);
        assertThrows(InvalidCredentialsException.class, () -> svc.authenticate(lr));

        // completeRegistration when username not found
        when(arepo.findByUsername("nouser")).thenReturn(Optional.empty());
        CompleteRegistrationRequest req = new CompleteRegistrationRequest("1000", Set.of(), false);
        assertThrows(AccountNotFoundException.class, () -> svc.completeRegistration("nouser", req));
    }

    @Test
    void authenticate_success_and_invalid_password() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        Account a = Account.builder().email("e@x").password(enc.encode("pw")).build();
        when(arepo.findByEmail("e@x")).thenReturn(Optional.of(a));

        LoginRequest good = new LoginRequest("e@x", "pw", false);
        Account ok = svc.authenticate(good);
        assertSame(a, ok);

        LoginRequest bad = new LoginRequest("e@x", "wrong", false);
        assertThrows(InvalidCredentialsException.class, () -> svc.authenticate(bad));
    }

    @Test
    void updateZipcode_and_completeRegistration() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        Account a = Account.builder().username("bob").build();
        when(arepo.findByUsername("bob")).thenReturn(Optional.of(a));
        when(arepo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account updated = svc.updateZipcode("bob", "3000");
        assertEquals("3000", updated.getZipcode());

        // completeRegistration: prepare conditions with communities
        Condition c1 = new Condition();
        c1.setName("Diabetes");
        Condition c2 = new Condition();
        c2.setName("Astma");
        when(crepo.findAllById(anyIterable())).thenReturn(List.of(c1, c2));

        CompleteRegistrationRequest req = new CompleteRegistrationRequest("1000", Set.of("Diabetes", "Astma"), true);
        Account comp = svc.completeRegistration("bob", req);
        assertEquals("1000", comp.getZipcode());
        assertTrue(comp.isHasCondition());
        assertNotNull(comp.getConditions());
        // updateZipcode and completeRegistration both call save -> expect two saves
        verify(arepo, times(2)).save(any(Account.class));
    }

    @Test
    void doesUsernameExist_username_exists() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        Account existingAccount = Account.builder().username("john").email("john@example.com").build();
        when(arepo.findByUsername("john")).thenReturn(Optional.of(existingAccount));

        boolean result = svc.doesUsernameExist("john");
        assertTrue(result);
        verify(arepo, times(1)).findByUsername("john");
    }

    @Test
    void doesUsernameExist_username_not_exists() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        when(arepo.findByUsername("newuser")).thenReturn(Optional.empty());

        boolean result = svc.doesUsernameExist("newuser");
        assertFalse(result);
        verify(arepo, times(1)).findByUsername("newuser");
    }

    @Test
    void doesEmailExist_email_exists() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        Account existingAccount = Account.builder().username("jane").email("jane@example.com").build();
        when(arepo.findByEmail("jane@example.com")).thenReturn(Optional.of(existingAccount));

        boolean result = svc.doesEmailExist("jane@example.com");
        assertTrue(result);
        verify(arepo, times(1)).findByEmail("jane@example.com");
    }

    @Test
    void doesEmailExist_email_not_exists() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        when(arepo.findByEmail("new@example.com")).thenReturn(Optional.empty());

        boolean result = svc.doesEmailExist("new@example.com");
        assertFalse(result);
        verify(arepo, times(1)).findByEmail("new@example.com");
    }

    @Test
    void findByUsername_success_and_not_found() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        Account existing = Account.builder().username("found").build();
        when(arepo.findByUsername("found")).thenReturn(Optional.of(existing));

        Account r = svc.findByUsername("found");
        assertSame(existing, r);

        when(arepo.findByUsername("missing")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> svc.findByUsername("missing"));
    }

    @Test
    void createAccount_password_null_or_blank() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        when(arepo.findByUsername(anyString())).thenReturn(Optional.empty());
        when(arepo.findByEmail(anyString())).thenReturn(Optional.empty());

        // null password
        RegisterRequest nullPw = new RegisterRequest("user", "user@example.com", null);
        assertThrows(IllegalArgumentException.class, () -> svc.createAccount(nullPw));

        // blank password
        RegisterRequest blankPw = new RegisterRequest("user", "user@example.com", "");
        assertThrows(IllegalArgumentException.class, () -> svc.createAccount(blankPw));

        RegisterRequest blankPw2 = new RegisterRequest("user", "user@example.com", "   ");
        assertThrows(IllegalArgumentException.class, () -> svc.createAccount(blankPw2));
    }

    @Test
    void createAccount_password_too_short() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        when(arepo.findByUsername(anyString())).thenReturn(Optional.empty());
        when(arepo.findByEmail(anyString())).thenReturn(Optional.empty());

        RegisterRequest shortPw = new RegisterRequest("user", "user@example.com", "Pass?1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> svc.createAccount(shortPw));
        assertTrue(ex.getMessage().contains("at least 8 characters"));
    }

    @Test
    void createAccount_password_no_uppercase() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        when(arepo.findByUsername(anyString())).thenReturn(Optional.empty());
        when(arepo.findByEmail(anyString())).thenReturn(Optional.empty());

        RegisterRequest noUpper = new RegisterRequest("user", "user@example.com", "password?123");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> svc.createAccount(noUpper));
        assertTrue(ex.getMessage().contains("uppercase"));
    }

    @Test
    void createAccount_password_no_special_character() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        when(arepo.findByUsername(anyString())).thenReturn(Optional.empty());
        when(arepo.findByEmail(anyString())).thenReturn(Optional.empty());

        RegisterRequest noSpecial = new RegisterRequest("user", "user@example.com", "Password123");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> svc.createAccount(noSpecial));
        assertTrue(ex.getMessage().contains("special character"));
    }

    @Test
    void updateZipcode_account_not_found() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        when(arepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> svc.updateZipcode("nonexistent", "1000"));
    }

    @Test
    void completeRegistration_with_communities() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        Account account = Account.builder().username("user").build();
        when(arepo.findByUsername("user")).thenReturn(Optional.of(account));
        when(arepo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // Create conditions with communities
        Condition condition1 = new Condition();
        condition1.setName("Diabetes");
        be.codeforbelgium.openinzichten.domain.Community community1 = new be.codeforbelgium.openinzichten.domain.Community();
        community1.setName("Diabetes Community");
        condition1.setCommunities(Set.of(community1));

        Condition condition2 = new Condition();
        condition2.setName("Hypertension");
        be.codeforbelgium.openinzichten.domain.Community community2 = new be.codeforbelgium.openinzichten.domain.Community();
        community2.setName("Heart Health Community");
        condition2.setCommunities(Set.of(community2));

        when(crepo.findAllById(anyIterable())).thenReturn(List.of(condition1, condition2));

        CompleteRegistrationRequest req = new CompleteRegistrationRequest("2000", Set.of("Diabetes", "Hypertension"), true);
        Account result = svc.completeRegistration("user", req);

        assertEquals("2000", result.getZipcode());
        assertTrue(result.isHasCondition());
        assertEquals(2, result.getConditions().size());
        assertEquals(2, result.getCommunities().size());
    }

    @Test
    void completeRegistration_with_null_communities() {
        AccountRepository arepo = mock(AccountRepository.class);
        ConditionRepository crepo = mock(ConditionRepository.class);
        AuthService svc = new AuthService(arepo, crepo);

        Account account = Account.builder().username("user").build();
        when(arepo.findByUsername("user")).thenReturn(Optional.of(account));
        when(arepo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // Create condition with null communities
        Condition condition = new Condition();
        condition.setName("SomeCondition");
        condition.setCommunities(null);

        when(crepo.findAllById(anyIterable())).thenReturn(List.of(condition));

        CompleteRegistrationRequest req = new CompleteRegistrationRequest("3000", Set.of("SomeCondition"), false);
        Account result = svc.completeRegistration("user", req);

        assertEquals("3000", result.getZipcode());
        assertFalse(result.isHasCondition());
        assertEquals(1, result.getConditions().size());
        assertEquals(0, result.getCommunities().size());
    }
}
