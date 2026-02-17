package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.request.CompleteRegistrationRequest;
import be.codeforbelgium.openinzichten.api.request.LoginRequest;
import be.codeforbelgium.openinzichten.api.request.RegisterRequest;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.Community;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException;
import be.codeforbelgium.openinzichten.exceptions.EmailTakenException;
import be.codeforbelgium.openinzichten.exceptions.InvalidCredentialsException;
import be.codeforbelgium.openinzichten.exceptions.UsernameTakenException;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.ConditionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final ConditionRepository conditionRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Account createAccount(RegisterRequest req) {
        // uniqueness checks
        if (accountRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new UsernameTakenException(req.getUsername());
        }
        if (accountRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new EmailTakenException(req.getEmail());
        }

        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }

        if (req.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if (!req.getPassword().matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        if (!req.getPassword().matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }

        Account toSave = Account.builder()
                .username(req.getUsername())
                .roles(Collections.singletonList("User"))
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .build();

        return accountRepository.save(toSave);
    }

    public Account updateZipcode(String username, String zipcode) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        account.setZipcode(zipcode);
        return accountRepository.save(account);
    }

    public Account authenticate(LoginRequest req) {

        Account account = accountRepository.findByEmail(req.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(req.getPassword(), account.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return account;
    }

    @Transactional
    public Account completeRegistration(String username, CompleteRegistrationRequest req) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        account.setZipcode(req.getZipcode());
        account.setHasCondition(req.isHasCondition());

        // Attach selected conditions
        Set<Condition> selected = new HashSet<>(conditionRepository.findAllById(req.getConditions()));
        account.setConditions(selected);

        // Derive communities from selected conditions
        Set<Community> derived = new HashSet<>();
        for (Condition c : selected) {
            if (c.getCommunities() != null) {
                derived.addAll(c.getCommunities());
            }
        }
        account.setCommunities(derived);

        return accountRepository.save(account);
    }

    public boolean doesUsernameExist(String username) {
        return accountRepository.findByUsername(username).isPresent();
    }

    public boolean doesEmailExist(String email) {
        return accountRepository.findByEmail(email).isPresent();
    }

    public Account findByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));
    }
}
