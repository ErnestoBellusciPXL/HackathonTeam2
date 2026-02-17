package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.ConnectionsResponse;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.domain.Connection;
import be.codeforbelgium.openinzichten.domain.ConnectionState;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.ConnectionRepository;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConnectionServiceTests {

    private Account mkAccount(UUID id, String username) {
        return Account.builder()
                .id(id)
                .username(username)
                .email(username + "@mail.test")
                .password("pw")
                .roles(List.of("USER"))
                .hasCondition(false)
                .build();
    }

    private Connection mkConnection(Account a, Account b, ConnectionState status) {
        return Connection.builder()
                .accountA(a)
                .accountB(b)
                .status(status)
                .build();
    }

    @Test
    void inviteToConnect_self_returnsCannotConnect() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        UUID id = UUID.randomUUID();
        var resp = svc.inviteToConnect(id, id);
        assertEquals("Cannot connect to self", resp.getMessage());
        verifyNoInteractions(accRepo, connRepo);
    }

    @Test
    void inviteToConnect_userNotFound_returnsMessage() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        when(accRepo.findById(any())).thenReturn(Optional.empty());
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.inviteToConnect(UUID.randomUUID(), UUID.randomUUID());
        assertEquals("User(s) not found", resp.getMessage());
        verifyNoInteractions(connRepo);
    }

    @Test
    void inviteToConnect_alreadyConnected_returnsAlreadyConnected() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Account from = mkAccount(fromId, "from");
        Account to = mkAccount(toId, "to");
        when(accRepo.findById(fromId)).thenReturn(Optional.of(from));
        when(accRepo.findById(toId)).thenReturn(Optional.of(to));
        when(connRepo.findBetween(fromId, toId)).thenReturn(Optional.of(mkConnection(from, to, ConnectionState.ACCEPTED)));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.inviteToConnect(fromId, toId);
        assertEquals("Already connected", resp.getMessage());
        verify(connRepo, never()).save(any());
    }

    @Test
    void inviteToConnect_pendingAlready_returnsPendingMessage() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Account from = mkAccount(fromId, "from");
        Account to = mkAccount(toId, "to");
        when(accRepo.findById(fromId)).thenReturn(Optional.of(from));
        when(accRepo.findById(toId)).thenReturn(Optional.of(to));
        when(connRepo.findBetween(fromId, toId)).thenReturn(Optional.of(mkConnection(from, to, ConnectionState.SENT)));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.inviteToConnect(fromId, toId);
        assertEquals("Connection request already pending", resp.getMessage());
    }

    @Test
    void inviteToConnect_previousDeclined_resetsAndSaves() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Account from = mkAccount(fromId, "from");
        Account to = mkAccount(toId, "to");
        when(accRepo.findById(fromId)).thenReturn(Optional.of(from));
        when(accRepo.findById(toId)).thenReturn(Optional.of(to));
        Connection existing = mkConnection(mkAccount(UUID.randomUUID(), "x"), mkAccount(UUID.randomUUID(), "y"), ConnectionState.DECLINED);
        when(connRepo.findBetween(fromId, toId)).thenReturn(Optional.of(existing));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.inviteToConnect(fromId, toId);
        assertEquals("Connection request sent", resp.getMessage());
        assertEquals(ConnectionState.SENT, existing.getStatus());
        assertEquals(fromId, existing.getAccountA().getId());
        assertEquals(toId, existing.getAccountB().getId());
        verify(connRepo).save(existing);
    }

    @Test
    void inviteToConnect_new_createsAndSaves() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Account from = mkAccount(fromId, "from");
        Account to = mkAccount(toId, "to");
        when(accRepo.findById(fromId)).thenReturn(Optional.of(from));
        when(accRepo.findById(toId)).thenReturn(Optional.of(to));
        when(connRepo.findBetween(fromId, toId)).thenReturn(Optional.empty());
        when(connRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.inviteToConnect(fromId, toId);
        assertEquals("Connection request sent", resp.getMessage());
        verify(connRepo, times(1)).save(any(Connection.class));
    }

    @Test
    void inviteToConnect_repositoryThrows_returnsFailed() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID fromId = UUID.randomUUID();
        when(accRepo.findById(fromId)).thenThrow(new RuntimeException("boom"));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.inviteToConnect(fromId, UUID.randomUUID());
        assertEquals("Failed to create connection", resp.getMessage());
    }

    @Test
    void getConnections_mapsAcceptedAndRequests() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        ConnectionService svc = new ConnectionService(accRepo, connRepo);

        UUID mainId = UUID.randomUUID();
        UUID otherAId = UUID.randomUUID();
        UUID otherBId = UUID.randomUUID();
        UUID otherCId = UUID.randomUUID();

        Account main = mkAccount(mainId, "main");
        Account otherA = mkAccount(otherAId, "otherA");
        Account otherB = mkAccount(otherBId, "otherB");
        Account otherC = mkAccount(otherCId, "otherC");

        // Conditions for otherA
        Condition c1 = new Condition();
        c1.setName("Diabetes");
        Condition c2 = new Condition();
        c2.setName("Asthma");
        otherA.setHasCondition(true);
        otherA.setConditions(new HashSet<>(List.of(c1, c2)));

        when(accRepo.findById(mainId)).thenReturn(Optional.of(main));

        // Build connections: ACCEPTED, SENT(viewer sender), SENT(viewer receiver), DECLINED
        Connection accepted = mkConnection(main, otherA, ConnectionState.ACCEPTED);
        accepted.setChatId(UUID.randomUUID());
        Connection sentViewerSender = mkConnection(main, otherB, ConnectionState.SENT);
        Connection sentViewerReceiver = mkConnection(otherC, main, ConnectionState.SENT);
        Connection declined = mkConnection(main, mkAccount(UUID.randomUUID(), "x"), ConnectionState.DECLINED);

        when(connRepo.findByAccountA_IdOrAccountB_Id(mainId, mainId))
                .thenReturn(List.of(accepted, sentViewerSender, sentViewerReceiver, declined));

        Optional<ConnectionsResponse> opt = svc.getConnections(mainId);
        assertTrue(opt.isPresent());
        ConnectionsResponse resp = opt.get();

        // One accepted connection
        assertEquals(1, resp.connections().size());
        assertEquals("otherA", resp.connections().get(0).username());
        assertEquals(List.of("Asthma", "Diabetes"), resp.connections().get(0).conditions());

        // Three requests: SENT, RECEIVED, DECLINED
        assertEquals(3, resp.requests().size());
        var states = new HashSet<String>();
        resp.requests().forEach(r -> states.add(r.state()));
        assertTrue(states.contains("SENT"));
        assertTrue(states.contains("RECEIVED"));
        assertTrue(states.contains("DECLINED"));
    }

    @Test
    void getConnections_accountNotFound_returnsEmptyOptional() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        when(accRepo.findById(any())).thenReturn(Optional.empty());
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        assertTrue(svc.getConnections(UUID.randomUUID()).isEmpty());
    }

    @Test
    void acceptConnection_self_returnsCannotAcceptSelf() {
        ConnectionService svc = new ConnectionService(mock(AccountRepository.class), mock(ConnectionRepository.class));
        UUID id = UUID.randomUUID();
        var resp = svc.acceptConnection(id, id);
        assertEquals("Cannot accept self", resp.getMessage());
    }

    @Test
    void acceptConnection_noUsersFound_returnsMessage() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        when(accRepo.findById(any())).thenReturn(Optional.empty());
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.acceptConnection(UUID.randomUUID(), UUID.randomUUID());
        assertEquals("User(s) not found", resp.getMessage());
    }

    @Test
    void acceptConnection_noIncomingRequest_whenNoConnectionFound() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        when(accRepo.findById(aId)).thenReturn(Optional.of(mkAccount(aId, "a")));
        when(accRepo.findById(bId)).thenReturn(Optional.of(mkAccount(bId, "b")));
        when(connRepo.findBetween(aId, bId)).thenReturn(Optional.empty());
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.acceptConnection(aId, bId);
        assertEquals("No incoming request to accept", resp.getMessage());
    }

    @Test
    void acceptConnection_noIncomingRequest_whenViewerIsSender() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        Account a = mkAccount(aId, "a");
        Account b = mkAccount(bId, "b");
        when(accRepo.findById(aId)).thenReturn(Optional.of(a));
        when(accRepo.findById(bId)).thenReturn(Optional.of(b));
        when(connRepo.findBetween(aId, bId)).thenReturn(Optional.of(mkConnection(a, b, ConnectionState.SENT)));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.acceptConnection(aId, bId);
        assertEquals("No incoming request to accept", resp.getMessage());
    }

    @Test
    void acceptConnection_success_setsAcceptedAndSaves() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        Account a = mkAccount(aId, "a");
        Account b = mkAccount(bId, "b");
        Connection existing = mkConnection(a, b, ConnectionState.SENT); // a is sender, so b accepts
        when(accRepo.findById(aId)).thenReturn(Optional.of(a));
        when(accRepo.findById(bId)).thenReturn(Optional.of(b));
        when(connRepo.findBetween(bId, aId)).thenReturn(Optional.of(existing)); // order doesn't matter in repo
        when(connRepo.findBetween(bId, aId)).thenReturn(Optional.of(existing));

        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.acceptConnection(bId, aId);
        assertEquals("Connection accepted", resp.getMessage());
        assertEquals(ConnectionState.ACCEPTED, existing.getStatus());
        verify(connRepo).save(existing);
    }

    @Test
    void acceptConnection_repositoryThrows_returnsFailed() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        when(accRepo.findById(aId)).thenThrow(new RuntimeException("fail"));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.acceptConnection(aId, UUID.randomUUID());
        assertEquals("Failed to accept connection", resp.getMessage());
    }

    @Test
    void rejectConnection_self_returnsCannotRejectSelf() {
        ConnectionService svc = new ConnectionService(mock(AccountRepository.class), mock(ConnectionRepository.class));
        UUID id = UUID.randomUUID();
        var resp = svc.rejectConnection(id, id);
        assertEquals("Cannot reject self", resp.getMessage());
    }

    @Test
    void rejectConnection_noIncomingRequest_whenViewerIsSenderOrStatusInvalid() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        Account a = mkAccount(aId, "a");
        Account b = mkAccount(bId, "b");
        when(accRepo.findById(aId)).thenReturn(Optional.of(a));
        when(accRepo.findById(bId)).thenReturn(Optional.of(b));
        when(connRepo.findBetween(aId, bId)).thenReturn(Optional.of(mkConnection(a, b, ConnectionState.SENT))); // viewer sender
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.rejectConnection(aId, bId);
        assertEquals("No incoming request to reject", resp.getMessage());
    }

    @Test
    void rejectConnection_success_setsDeclined() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        Account a = mkAccount(aId, "a");
        Account b = mkAccount(bId, "b");
        Connection existing = mkConnection(a, b, ConnectionState.SENT); // a sent, b rejects
        when(accRepo.findById(aId)).thenReturn(Optional.of(a));
        when(accRepo.findById(bId)).thenReturn(Optional.of(b));
        when(connRepo.findBetween(bId, aId)).thenReturn(Optional.of(existing));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.rejectConnection(bId, aId);
        assertEquals("Connection rejected", resp.getMessage());
        assertEquals(ConnectionState.DECLINED, existing.getStatus());
        verify(connRepo).save(existing);
    }

    @Test
    void rejectConnection_repositoryThrows_returnsFailed() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        when(accRepo.findById(aId)).thenThrow(new RuntimeException("boom"));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.rejectConnection(aId, UUID.randomUUID());
        assertEquals("Failed to reject connection", resp.getMessage());
    }

    @Test
    void disconnect_self_returnsCannotDisconnectSelf() {
        ConnectionService svc = new ConnectionService(mock(AccountRepository.class), mock(ConnectionRepository.class));
        UUID id = UUID.randomUUID();
        var resp = svc.disconnect(id, id);
        assertEquals("Cannot disconnect self", resp.getMessage());
    }

    @Test
    void disconnect_usersNotFound_returnsMessage() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        when(accRepo.findById(any())).thenReturn(Optional.empty());
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.disconnect(UUID.randomUUID(), UUID.randomUUID());
        assertEquals("User(s) not found", resp.getMessage());
    }

    @Test
    void disconnect_noActiveConnection_returnsMessage() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        when(accRepo.findById(aId)).thenReturn(Optional.of(mkAccount(aId, "a")));
        when(accRepo.findById(bId)).thenReturn(Optional.of(mkAccount(bId, "b")));
        when(connRepo.findBetween(aId, bId)).thenReturn(Optional.of(mkConnection(mkAccount(aId, "a"), mkAccount(bId, "b"), ConnectionState.SENT)));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.disconnect(aId, bId);
        assertEquals("No active connection found", resp.getMessage());
    }

    @Test
    void disconnect_success_setsDeclined() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        Account a = mkAccount(aId, "a");
        Account b = mkAccount(bId, "b");
        Connection existing = mkConnection(a, b, ConnectionState.ACCEPTED);
        when(accRepo.findById(aId)).thenReturn(Optional.of(a));
        when(accRepo.findById(bId)).thenReturn(Optional.of(b));
        when(connRepo.findBetween(aId, bId)).thenReturn(Optional.of(existing));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.disconnect(aId, bId);
        assertEquals("Disconnected", resp.getMessage());
        assertEquals(ConnectionState.DECLINED, existing.getStatus());
        verify(connRepo).save(existing);
    }

    @Test
    void disconnect_repositoryThrows_returnsFailed() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        when(accRepo.findById(aId)).thenThrow(new RuntimeException("boom"));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.disconnect(aId, UUID.randomUUID());
        assertEquals("Failed to disconnect", resp.getMessage());
    }

    @Test
    void cancelInvite_self_returnsCannotCancelSelf() {
        ConnectionService svc = new ConnectionService(mock(AccountRepository.class), mock(ConnectionRepository.class));
        UUID id = UUID.randomUUID();
        var resp = svc.cancelInvite(id, id);
        assertEquals("Cannot cancel self", resp.getMessage());
    }

    @Test
    void cancelInvite_noUsersFound_returnsMessage() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        when(accRepo.findById(any())).thenReturn(Optional.empty());
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.cancelInvite(UUID.randomUUID(), UUID.randomUUID());
        assertEquals("User(s) not found", resp.getMessage());
    }

    @Test
    void cancelInvite_noOutgoingRequest_returnsMessage() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        Account a = mkAccount(aId, "a");
        Account b = mkAccount(bId, "b");
        when(accRepo.findById(aId)).thenReturn(Optional.of(a));
        when(accRepo.findById(bId)).thenReturn(Optional.of(b));
        when(connRepo.findBetween(aId, bId)).thenReturn(Optional.of(mkConnection(a, b, ConnectionState.DECLINED))); // not SENT
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.cancelInvite(aId, bId);
        assertEquals("No outgoing request to cancel", resp.getMessage());
    }

    @Test
    void cancelInvite_success_updatesStatesAndSaves() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        Account a = mkAccount(aId, "a");
        Account b = mkAccount(bId, "b");
        Connection existing = mkConnection(a, b, ConnectionState.SENT); // a is sender, cancels
        when(accRepo.findById(aId)).thenReturn(Optional.of(a));
        when(accRepo.findById(bId)).thenReturn(Optional.of(b));
        when(connRepo.findBetween(aId, bId)).thenReturn(Optional.of(existing));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.cancelInvite(aId, bId);
        assertEquals("Connection request cancelled", resp.getMessage());
        assertEquals(ConnectionState.DECLINED, existing.getStatus());
        verify(connRepo).save(existing);
    }

    @Test
    void cancelInvite_repositoryThrows_returnsFailed() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID aId = UUID.randomUUID();
        when(accRepo.findById(aId)).thenThrow(new RuntimeException("boom"));
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        var resp = svc.cancelInvite(aId, UUID.randomUUID());
        assertEquals("Failed to cancel connection request", resp.getMessage());
    }

    @Test
    void getConnections_emptyList_returnsEmptyCollections() {
        AccountRepository accRepo = mock(AccountRepository.class);
        ConnectionRepository connRepo = mock(ConnectionRepository.class);
        UUID mainId = UUID.randomUUID();
        when(accRepo.findById(mainId)).thenReturn(Optional.of(mkAccount(mainId, "main")));
        when(connRepo.findByAccountA_IdOrAccountB_Id(mainId, mainId)).thenReturn(List.of());
        ConnectionService svc = new ConnectionService(accRepo, connRepo);
        Optional<ConnectionsResponse> opt = svc.getConnections(mainId);
        assertTrue(opt.isPresent());
        var cr = opt.get();
        assertTrue(cr.connections().isEmpty());
        assertTrue(cr.requests().isEmpty());
    }
}
