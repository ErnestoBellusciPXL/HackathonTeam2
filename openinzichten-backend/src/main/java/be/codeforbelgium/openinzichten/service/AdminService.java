package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.AdminAccountResponse;
import be.codeforbelgium.openinzichten.domain.Community;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import be.codeforbelgium.openinzichten.api.response.AdminAccountDetailResponse;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException;

@Service
@RequiredArgsConstructor
public class AdminService {

        private final AccountRepository accountRepository;
        private final MailService mailService;

    public Page<AdminAccountResponse> getAllAccounts(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("username").ascending());

        return accountRepository.findAll(pageable)
                .map(account -> new AdminAccountResponse(
                        account.getId().toString(),
                        account.getUsername(),
                        account.isDisabled(),
                        account.isHasCondition(),
                        account.getConditions() != null ? account.getConditions().stream()
                                .map(Condition::getName)
                                .toList() : List.of(),
                        account.getCommunities() != null ? account.getCommunities().stream()
                                .map(Community::getName)
                                .toList() : List.of()));
    }

    public AdminAccountDetailResponse getAccountById(String id) {
        UUID uuid = UUID.fromString(id);
        Account account = accountRepository.findById(uuid)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + id));

        return new AdminAccountDetailResponse(
                account.getId().toString(),
                account.getUsername(),
                account.getEmail(),
                account.getZipcode(),
                account.isDisabled(),
                account.getConditions() != null ? account.getConditions().stream().map(c -> c.getName()).toList() : List.of(),
                account.getDisabledReason(),
                account.getDisabledAt()
        );
    }

        public AdminAccountDetailResponse setAccountDisabled(String id, boolean disabled, String reason) {
        UUID uuid = UUID.fromString(id);
        Account account = accountRepository.findById(uuid)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + id));

        boolean wasDisabled = account.isDisabled();
        account.setDisabled(disabled);
        if (disabled) {
            account.setDisabledReason(reason);
            account.setDisabledAt(Instant.now());
        } else {
            account.setDisabledReason(null);
            account.setDisabledAt(null);
        }
        accountRepository.save(account);

        // Send notification email when account is disabled or reactivated
        if (account.getEmail() != null && !account.getEmail().isBlank()) {
            try {
                if (disabled) {
                    mailService.sendAccountDisabled(account.getEmail(), account.getUsername(), reason);
                } else if (wasDisabled) {
                    mailService.sendAccountReactivated(account.getEmail(), account.getUsername());
                }
            } catch (Exception ex) {
                // Log and continue; do not fail the user update due to mail errors
                org.slf4j.LoggerFactory.getLogger(AdminService.class)
                        .error("Failed to send account status email to {}", account.getEmail(), ex);
            }
        }

        return new AdminAccountDetailResponse(
                account.getId().toString(),
                account.getUsername(),
                account.getEmail(),
                account.getZipcode(),
                account.isDisabled(),
                account.getConditions() != null ? account.getConditions().stream().map(c -> c.getName()).toList() : List.of(),
                account.getDisabledReason(),
                account.getDisabledAt()
        );
    }
}
