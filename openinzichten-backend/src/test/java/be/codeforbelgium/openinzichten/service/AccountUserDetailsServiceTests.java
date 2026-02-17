package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountUserDetailsServiceTests {

    @Test
    void loadUserByUsername_success() {
        AccountRepository repo = mock(AccountRepository.class);
        AccountUserDetailsService svc = new AccountUserDetailsService(repo);

        Account a = Account.builder()
                .username("bob")
                .password("encoded")
                .roles(List.of("ROLE_USER"))
                .build();
        when(repo.findByUsername("bob")).thenReturn(Optional.of(a));

        UserDetails ud = svc.loadUserByUsername("bob");
        assertEquals("bob", ud.getUsername());
        assertEquals("encoded", ud.getPassword());
        assertTrue(ud.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_not_found() {
        AccountRepository repo = mock(AccountRepository.class);
        AccountUserDetailsService svc = new AccountUserDetailsService(repo);

        when(repo.findByUsername("x")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> svc.loadUserByUsername("x"));
    }
}
