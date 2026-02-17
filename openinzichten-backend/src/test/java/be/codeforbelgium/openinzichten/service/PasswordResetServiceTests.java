package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.request.PasswordResetConfirmRequest;
import be.codeforbelgium.openinzichten.api.request.PasswordResetRequest;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.PasswordResetToken;
import be.codeforbelgium.openinzichten.exceptions.PasswordResetTokenExpiredException;
import be.codeforbelgium.openinzichten.exceptions.PasswordResetTokenNotFoundException;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PasswordResetServiceTests {

    private AccountRepository accountRepo;
    private PasswordResetTokenRepository tokenRepo;
    private MailService mailService;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        accountRepo = mock(AccountRepository.class);
        tokenRepo = mock(PasswordResetTokenRepository.class);
        mailService = mock(MailService.class);
        service = new PasswordResetService(accountRepo, tokenRepo, mailService);
        ReflectionTestUtils.setField(service, "frontendResetUrl", "http://front?token=");
    }

    @Test
    void requestPasswordReset_sends_email_and_saves_token() {
        Account acc = Account.builder().email("a@b").build();
        when(accountRepo.findByEmail("a@b")).thenReturn(Optional.of(acc));

        service.requestPasswordReset(new PasswordResetRequest("a@b"));

        verify(tokenRepo).save(any(PasswordResetToken.class));
        verify(mailService).sendPasswordReset(eq("a@b"), contains("http://front?token="));
    }

    @Test
    void resetPassword_success_and_errors() {
        Account acc = Account.builder().email("u@e").build();
        PasswordResetToken prt = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .token("t1")
                .account(acc)
                .expiry(Instant.now().plusSeconds(3600))
                .build();

        when(tokenRepo.findByToken("t1")).thenReturn(Optional.of(prt));

        PasswordResetConfirmRequest req = new PasswordResetConfirmRequest("t1", "newpass");
        service.resetPassword(req);

        verify(accountRepo).save(any(Account.class));
        verify(tokenRepo).deleteById(prt.getId());

        // expired token
        PasswordResetToken expired = PasswordResetToken.builder().token("t2").expiry(Instant.now().minusSeconds(10)).id(UUID.randomUUID()).account(acc).build();
        when(tokenRepo.findByToken("t2")).thenReturn(Optional.of(expired));
        PasswordResetConfirmRequest expiredReq = new PasswordResetConfirmRequest("t2", "p");
        assertThrows(PasswordResetTokenExpiredException.class, () -> service.resetPassword(expiredReq));

        // not found
        when(tokenRepo.findByToken("missing")).thenReturn(Optional.empty());
        PasswordResetConfirmRequest missingReq = new PasswordResetConfirmRequest("missing", "p");
        assertThrows(PasswordResetTokenNotFoundException.class, () -> service.resetPassword(missingReq));
    }
}
