package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.request.PasswordResetConfirmRequest;
import be.codeforbelgium.openinzichten.api.request.PasswordResetRequest;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.PasswordResetToken;
import be.codeforbelgium.openinzichten.exceptions.PasswordResetTokenExpiredException;
import be.codeforbelgium.openinzichten.exceptions.PasswordResetTokenNotFoundException;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    // tokens valid for 1 hour
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(1);
    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Value("${app.frontend.reset-url:http://localhost:3000/reset-password?token=}")
    private String frontendResetUrl;

    public void requestPasswordReset(PasswordResetRequest req) {
        Account account = accountRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Account not found for email: " + req.getEmail()));

        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token)
                .account(account)
                .expiry(Instant.now().plus(TOKEN_VALIDITY))
                .build();

        tokenRepository.save(prt);

        // Build reset link using frontend URL configured in application.properties
        String resetLink = frontendResetUrl + token;
        mailService.sendPasswordReset(account.getEmail(), resetLink);
    }

    public void resetPassword(PasswordResetConfirmRequest req) {
        PasswordResetToken prt = tokenRepository.findByToken(req.getToken())
                .orElseThrow(() -> new PasswordResetTokenNotFoundException(req.getToken()));

        if (prt.getExpiry().isBefore(Instant.now())) {
            throw new PasswordResetTokenExpiredException(req.getToken());
        }

        Account account = prt.getAccount();
        account.setPassword(passwordEncoder.encode(req.getNewPassword()));
        accountRepository.save(account);

        // remove token after successful reset
        tokenRepository.deleteById(prt.getId());
    }

}
