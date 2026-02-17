package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.ConnectionAccountResponse;
import be.codeforbelgium.openinzichten.api.response.ConnectionRequestResponse;
import be.codeforbelgium.openinzichten.api.response.ConnectionsResponse;
import be.codeforbelgium.openinzichten.api.response.InviteConnectionResponse;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.domain.Connection;
import be.codeforbelgium.openinzichten.domain.ConnectionState;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectionService {
    private static final Logger log = LoggerFactory.getLogger(ConnectionService.class);
    private static final String USER_NOT_FOUND = "User(s) not found";
    private final AccountRepository accountRepository;
    private final ConnectionRepository connectionRepository;

    public InviteConnectionResponse inviteToConnect(UUID fromId, UUID toId) {
        try {
            if (fromId.equals(toId))
                return new InviteConnectionResponse("Cannot connect to self");

            Optional<Account> fromOpt = accountRepository.findById(fromId);
            Optional<Account> toOpt = accountRepository.findById(toId);

            if (fromOpt.isEmpty() || toOpt.isEmpty())
                return new InviteConnectionResponse(USER_NOT_FOUND);

            Account from = fromOpt.get();
            Account to = toOpt.get();

            Optional<Connection> existingOpt = connectionRepository.findBetween(fromId, toId);

            if (existingOpt.isPresent()) {
                Connection existing = existingOpt.get();
                if (existing.getStatus() == ConnectionState.ACCEPTED) {
                    return new InviteConnectionResponse("Already connected");
                }
                if (existing.getStatus() == ConnectionState.SENT) {
                    return new InviteConnectionResponse("Connection request already pending");
                }
                if (existing.getStatus() == ConnectionState.DECLINED) {
                    existing.setAccountA(from);
                    existing.setAccountB(to);
                    existing.setStatus(ConnectionState.SENT);
                    connectionRepository.save(existing);
                    return new InviteConnectionResponse("Connection request sent");
                }
            }

            Connection conn = Connection.builder()
                    .accountA(from)
                    .accountB(to)
                    .status(ConnectionState.SENT)
                    .build();
            connectionRepository.save(conn);
            return new InviteConnectionResponse("Connection request sent");
        } catch (Exception e) {
            log.error("Failed to create connection between {} and {}: {}", fromId, toId, e.getMessage());
            log.debug("Full stacktrace for create connection failure between {} and {}", fromId, toId, e);
            return new InviteConnectionResponse("Failed to create connection");
        }
    }

    public Optional<ConnectionsResponse> getConnections(UUID accountId) {
        return accountRepository.findById(accountId).map(acc -> {
            List<Connection> all = connectionRepository.findByAccountA_IdOrAccountB_Id(accountId, accountId);

            List<ConnectionAccountResponse> connected = buildConnectedResponses(acc, all);
            List<ConnectionRequestResponse> requests = buildRequestResponses(acc, all);

            return new ConnectionsResponse(connected, requests);
        });
    }

    private List<ConnectionAccountResponse> buildConnectedResponses(Account acc, List<Connection> all) {
        return all.stream()
                .filter(c -> c.getStatus() == ConnectionState.ACCEPTED)
                .map(c -> {
                    Account other = acc.getId().equals(c.getAccountA().getId()) ? c.getAccountB() : c.getAccountA();
                    List<String> conditions = other.getConditions() == null ? List.of()
                            : other.getConditions().stream()
                            .map(Condition::getName)
                            .sorted()
                            .toList();
                    return new ConnectionAccountResponse(
                            other.getId().toString(),
                            other.getUsername(),
                            other.isHasCondition(),
                            conditions,
                            c.getChatId());
                })
                .toList();
    }

    private List<ConnectionRequestResponse> buildRequestResponses(Account acc, List<Connection> all) {
        return all.stream()
                .filter(c -> c.getStatus() == ConnectionState.SENT || c.getStatus() == ConnectionState.DECLINED)
                .map(c -> {
                    boolean viewerIsSender = acc.getId().equals(c.getAccountA().getId());
                    Account other = viewerIsSender ? c.getAccountB() : c.getAccountA();
                    String stateStr;
                    if (c.getStatus() == ConnectionState.SENT) {
                        stateStr = viewerIsSender ? ConnectionState.SENT.name() : ConnectionState.RECEIVED.name();
                    } else {
                        stateStr = ConnectionState.DECLINED.name();
                    }
                    return new ConnectionRequestResponse(
                            other.getId().toString(),
                            other.getUsername(),
                            stateStr);
                })
                .toList();
    }

    public InviteConnectionResponse acceptConnection(UUID accountId, UUID otherId) {
        try {
            if (accountId.equals(otherId))
                return new InviteConnectionResponse("Cannot accept self");

            Optional<Account> accOpt = accountRepository.findById(accountId);
            Optional<Account> otherOpt = accountRepository.findById(otherId);
            if (accOpt.isEmpty() || otherOpt.isEmpty())
                return new InviteConnectionResponse(USER_NOT_FOUND);

            Optional<Connection> existingOpt = connectionRepository.findBetween(accountId, otherId);
            if (existingOpt.isEmpty()) {
                return new InviteConnectionResponse("No incoming request to accept");
            }
            Connection existing = existingOpt.get();
            boolean viewerIsSender = accountId.equals(existing.getAccountA().getId());
            if (existing.getStatus() != ConnectionState.SENT || viewerIsSender) {
                return new InviteConnectionResponse("No incoming request to accept");
            }
            existing.setStatus(ConnectionState.ACCEPTED);
            connectionRepository.save(existing);
            return new InviteConnectionResponse("Connection accepted");
        } catch (Exception e) {
            log.error("Failed to accept connection between {} and {}: {}", accountId, otherId, e.getMessage());
            log.debug("Full stacktrace for accept connection failure between {} and {}", accountId, otherId, e);
            return new InviteConnectionResponse("Failed to accept connection");
        }
    }

    public InviteConnectionResponse rejectConnection(UUID accountId, UUID otherId) {
        try {
            if (accountId.equals(otherId))
                return new InviteConnectionResponse("Cannot reject self");

            Optional<Account> accOpt = accountRepository.findById(accountId);
            Optional<Account> otherOpt = accountRepository.findById(otherId);
            if (accOpt.isEmpty() || otherOpt.isEmpty())
                return new InviteConnectionResponse(USER_NOT_FOUND);

            Optional<Connection> existingOpt = connectionRepository.findBetween(accountId, otherId);
            if (existingOpt.isEmpty()) {
                return new InviteConnectionResponse("No incoming request to reject");
            }
            Connection existing = existingOpt.get();
            boolean viewerIsSender = accountId.equals(existing.getAccountA().getId());
            if (existing.getStatus() != ConnectionState.SENT || viewerIsSender) {
                return new InviteConnectionResponse("No incoming request to reject");
            }
            existing.setStatus(ConnectionState.DECLINED);
            connectionRepository.save(existing);
            return new InviteConnectionResponse("Connection rejected");
        } catch (Exception e) {
            log.error("Failed to reject connection between {} and {}: {}", accountId, otherId, e.getMessage());
            log.debug("Full stacktrace for reject connection failure between {} and {}", accountId, otherId, e);
            return new InviteConnectionResponse("Failed to reject connection");
        }
    }

    public InviteConnectionResponse disconnect(UUID accountId, UUID otherId) {
        try {
            if (accountId.equals(otherId))
                return new InviteConnectionResponse("Cannot disconnect self");

            Optional<Account> accOpt = accountRepository.findById(accountId);
            Optional<Account> otherOpt = accountRepository.findById(otherId);
            if (accOpt.isEmpty() || otherOpt.isEmpty())
                return new InviteConnectionResponse(USER_NOT_FOUND);

            Optional<Connection> existingOpt = connectionRepository.findBetween(accountId, otherId);
            if (existingOpt.isEmpty() || existingOpt.get().getStatus() != ConnectionState.ACCEPTED) {
                return new InviteConnectionResponse("No active connection found");
            }
            Connection existing = existingOpt.get();
            existing.setStatus(ConnectionState.DECLINED);
            connectionRepository.save(existing);
            return new InviteConnectionResponse("Disconnected");
        } catch (Exception e) {
            log.error("Failed to disconnect between {} and {}: {}", accountId, otherId, e.getMessage());
            log.debug("Full stacktrace for disconnect failure between {} and {}", accountId, otherId, e);
            return new InviteConnectionResponse("Failed to disconnect");
        }
    }

    public InviteConnectionResponse cancelInvite(UUID accountId, UUID otherId) {
        try {
            if (accountId.equals(otherId))
                return new InviteConnectionResponse("Cannot cancel self");

            Optional<Account> accOpt = accountRepository.findById(accountId);
            Optional<Account> otherOpt = accountRepository.findById(otherId);
            if (accOpt.isEmpty() || otherOpt.isEmpty())
                return new InviteConnectionResponse(USER_NOT_FOUND);

            Optional<Connection> existingOpt = connectionRepository.findBetween(accountId, otherId);
            if (existingOpt.isEmpty()) {
                return new InviteConnectionResponse("No outgoing request to cancel");
            }
            Connection existing = existingOpt.get();
            boolean viewerIsSender = accountId.equals(existing.getAccountA().getId());
            if (existing.getStatus() != ConnectionState.SENT || !viewerIsSender) {
                return new InviteConnectionResponse("No outgoing request to cancel");
            }
            existing.setStatus(ConnectionState.DECLINED);
            connectionRepository.save(existing);
            return new InviteConnectionResponse("Connection request cancelled");
        } catch (Exception e) {
            log.error("Failed to cancel connection request between {} and {}: {}", accountId, otherId, e.getMessage());
            log.debug("Full stacktrace for cancel invite failure between {} and {}", accountId, otherId, e);
            return new InviteConnectionResponse("Failed to cancel connection request");
        }
    }
}
