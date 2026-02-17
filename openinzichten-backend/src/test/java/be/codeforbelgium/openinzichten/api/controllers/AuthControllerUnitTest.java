package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.request.LoginRequest;
import be.codeforbelgium.openinzichten.api.request.RegisterRequest;
import be.codeforbelgium.openinzichten.api.response.AuthResponse;
import be.codeforbelgium.openinzichten.api.response.MessageResponse;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.security.JwtService;
import be.codeforbelgium.openinzichten.service.AuthService;
import be.codeforbelgium.openinzichten.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthControllerUnitTest {

    @Test
    void me_returnsAccountStatus() {
        AuthService authService = Mockito.mock(AuthService.class);
        JwtService jwt = Mockito.mock(JwtService.class);
        PasswordResetService prs = Mockito.mock(PasswordResetService.class);

        var controller = new AuthController(authService, jwt, prs);

        var account = new Account();
        account.setId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        account.setUsername("meuser");
        account.setDisabled(true);
        when(authService.findByUsername("meuser")).thenReturn(account);

        Principal p = () -> "meuser";
        var resp = controller.me(p);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().username()).isEqualTo("meuser");
        assertThat(resp.getBody().disabled()).isTrue();
    }

    @Test
    void register_createsAccount_and_returnsToken() {
        AuthService authService = Mockito.mock(AuthService.class);
        JwtService jwt = Mockito.mock(JwtService.class);
        PasswordResetService prs = Mockito.mock(PasswordResetService.class);

        var controller = new AuthController(authService, jwt, prs);

        var req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("pass");
        req.setEmail("e@e.com");

        var account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("newuser");
        account.setRoles(List.of());

        when(authService.createAccount(req)).thenReturn(account);
        when(jwt.generateToken(any(), any(), any(), ArgumentMatchers.anyList(), ArgumentMatchers.anyBoolean())).thenReturn("tok-123");

        var resp = controller.register(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        assertThat(((AuthResponse) resp.getBody()).token()).isEqualTo("tok-123");
    }

    @Test
    void login_disabledAccount_returns403WithReason() {
        AuthService authService = Mockito.mock(AuthService.class);
        JwtService jwt = Mockito.mock(JwtService.class);
        PasswordResetService prs = Mockito.mock(PasswordResetService.class);

        var controller = new AuthController(authService, jwt, prs);

        var acc = new Account();
        acc.setDisabled(true);
        acc.setDisabledReason("spam");

        var req = new LoginRequest();
        req.setEmail("u@example.com");
        req.setPassword("p");
        when(authService.authenticate(req)).thenReturn(acc);

        var resp = controller.login(req);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
        assertThat(((MessageResponse) resp.getBody()).getMessage()).isEqualTo("spam");
    }
}
