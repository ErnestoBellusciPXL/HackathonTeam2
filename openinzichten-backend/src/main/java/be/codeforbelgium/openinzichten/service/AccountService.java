package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest;
import be.codeforbelgium.openinzichten.api.response.UserAccountResponse;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.domain.Ticket;
import be.codeforbelgium.openinzichten.domain.TicketState;
import be.codeforbelgium.openinzichten.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final ConnectionRepository connectionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TicketRepository ticketRepository;
    private final ConditionRepository conditionRepository;

    @Transactional
    public boolean deleteAccountById(UUID id, String password) {
        var account = accountRepository.findById(id);

        if (account.isEmpty() || password == null || !passwordEncoder.matches(password, account.get().getPassword())) {
            return false;
        }

        detachTicketsForDeletedAccount(id);
        chatMessageRepository.deleteBySenderId(id);
        var connections = connectionRepository.findByAccountA_IdOrAccountB_Id(id, id);
        if (!connections.isEmpty()) {
            connectionRepository.deleteAll(connections);
        }

        accountRepository.delete(account.get());
        return true;
    }

    private void detachTicketsForDeletedAccount(UUID accountId) {
        List<Ticket> reporterTickets = ticketRepository.findAllByReporterId(accountId);
        List<Ticket> reporteeTickets = ticketRepository.findAllByReporteeId(accountId);
        List<Ticket> storyTickets = ticketRepository.findAllByStoryOwnerId(accountId);

        Set<Ticket> dirty = new HashSet<>();

        if (!reporterTickets.isEmpty()) {
            reporterTickets.forEach(t -> t.setReporter(null));
            dirty.addAll(reporterTickets);
        }

        if (!storyTickets.isEmpty()) {
            storyTickets.forEach(ticket -> {
                ticket.setState(TicketState.CLOSED);
                ticket.setStory(null);
                ticket.setReportee(null);
            });
            dirty.addAll(storyTickets);
        }

        if (!reporteeTickets.isEmpty()) {
            reporteeTickets.forEach(t -> t.setReportee(null));
            dirty.addAll(reporteeTickets);
        }

        if (!dirty.isEmpty()) {
            ticketRepository.saveAllAndFlush(dirty);
        }
    }


    public UpdateAccountResult updateAccount(UUID userId, UpdateAccountRequest req) {
        var account = accountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        boolean usernameChanged = updateUsername(account, req);
        boolean emailChanged = updateEmail(account, req);
        updateZipcode(account, req);
        updateConditions(account, req);
        updateHasCondition(account, req);

        var updatedAccount = accountRepository.save(account);

        var accountResponse = new UserAccountResponse(
                updatedAccount.getUsername(),
                updatedAccount.getEmail(),
                updatedAccount.getZipcode(),
                updatedAccount.isHasCondition(),
                updatedAccount.getConditions().stream().map(Condition::getName).sorted().toList());

        return new UpdateAccountResult(accountResponse, usernameChanged, emailChanged, updatedAccount.getRoles());
    }

    public UserAccountResponse getAccountById(UUID userId) {
        var accountOpt = accountRepository.findById(userId);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found");
        }
        var account = accountOpt.get();

        return new UserAccountResponse(
                account.getUsername(),
                account.getEmail(),
                account.getZipcode(),
                account.isHasCondition(),
                account.getConditions().stream().map(Condition::getName).sorted().toList());
    }

    private boolean updateUsername(Account account, UpdateAccountRequest req) {
        if (!hasText(req.getUsername())) {
            return false;
        }

        String newUsername = req.getUsername().trim();
        if (newUsername.equals(account.getUsername())) {
            return false;
        }

        var existingByUsername = accountRepository.findByUsername(newUsername);
        if (existingByUsername.isPresent() && !existingByUsername.get().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Username already taken");
        }

        account.setUsername(newUsername);
        return true;
    }

    private boolean updateEmail(Account account, UpdateAccountRequest req) {
        if (!hasText(req.getEmail())) {
            return false;
        }

        String newEmail = req.getEmail().trim();
        if (newEmail.equals(account.getEmail())) {
            return false;
        }

        var existingByEmail = accountRepository.findByEmail(newEmail);
        if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Email already in use");
        }

        account.setEmail(newEmail);
        return true;
    }

    private void updateZipcode(Account account, UpdateAccountRequest req) {
        if (hasText(req.getZipcode())) {
            account.setZipcode(req.getZipcode().trim());
        }
    }

    private void updateConditions(Account account, UpdateAccountRequest req) {
        if (req.getConditions() == null) {
            return;
        }

        Set<Condition> userConditions = account.getConditions();
        if (userConditions == null) {
            userConditions = new HashSet<>();
        }
        Set<Condition> requestedConditions = new HashSet<>();

        for (String conditionName : req.getConditions()) {
            conditionRepository.findByName(conditionName).ifPresent(requestedConditions::add);
        }

        userConditions.retainAll(requestedConditions);

        requestedConditions.removeAll(userConditions);
        userConditions.addAll(requestedConditions);

        account.setConditions(userConditions);
    }

    private void updateHasCondition(Account account, UpdateAccountRequest req) {
        if (req.getHasCondition() != null) {
            account.setHasCondition(req.getHasCondition());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }



}


