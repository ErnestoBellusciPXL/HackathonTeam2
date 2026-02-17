package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.request.InviteConnectionRequest;
import be.codeforbelgium.openinzichten.api.response.ConnectionAccountResponse;
import be.codeforbelgium.openinzichten.api.response.ConnectionRequestResponse;
import be.codeforbelgium.openinzichten.api.response.ConnectionsResponse;
import be.codeforbelgium.openinzichten.api.response.InviteConnectionResponse;
import be.codeforbelgium.openinzichten.service.ConnectionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Controller tests for {@link ConnectionController}. We call controller methods directly (same pattern as other controller tests)
 * and mock {@link ConnectionService} to cover success, not found, and error paths.
 */
class ConnectionControllerTests {

    @Test
    void invite_success_returns200() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.inviteToConnect(any(), any())).thenReturn(new InviteConnectionResponse("Connection request sent"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.invite(req);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Connection request sent", resp.getBody().getMessage());
    }

    @Test
    void invite_serviceThrows_returns500() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.inviteToConnect(any(), any())).thenThrow(new RuntimeException("boom"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.invite(req);
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void getConnections_found_returns200() {
        ConnectionService svc = mock(ConnectionService.class);
        ConnectionController controller = new ConnectionController(svc);
        UUID accountId = UUID.randomUUID();
        ConnectionsResponse response = new ConnectionsResponse(
                List.of(new ConnectionAccountResponse("id1", "user1", true, List.of("Asthma"), null)),
                List.of(new ConnectionRequestResponse("id2", "user2", "SENT"))
        );
        when(svc.getConnections(accountId)).thenReturn(Optional.of(response));
        var resp = controller.getConnections(accountId);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().connections().size());
    }

    @Test
    void getConnections_notFound_returns404() {
        ConnectionService svc = mock(ConnectionService.class);
        ConnectionController controller = new ConnectionController(svc);
        UUID accountId = UUID.randomUUID();
        when(svc.getConnections(accountId)).thenReturn(Optional.empty());
        var resp = controller.getConnections(accountId);
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void accept_success_returns200() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.acceptConnection(any(), any())).thenReturn(new InviteConnectionResponse("Connection accepted"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.accept(req);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Connection accepted", resp.getBody().getMessage());
    }

    @Test
    void accept_serviceThrows_returns500() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.acceptConnection(any(), any())).thenThrow(new RuntimeException("boom"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.accept(req);
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void reject_success_returns200() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.rejectConnection(any(), any())).thenReturn(new InviteConnectionResponse("Connection rejected"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.reject(req);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Connection rejected", resp.getBody().getMessage());
    }

    @Test
    void reject_serviceThrows_returns500() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.rejectConnection(any(), any())).thenThrow(new RuntimeException("boom"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.reject(req);
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void disconnect_success_returns200() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.disconnect(any(), any())).thenReturn(new InviteConnectionResponse("Disconnected"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.disconnect(req);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Disconnected", resp.getBody().getMessage());
    }

    @Test
    void disconnect_serviceThrows_returns500() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.disconnect(any(), any())).thenThrow(new RuntimeException("boom"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.disconnect(req);
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void cancel_success_returns200() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.cancelInvite(any(), any())).thenReturn(new InviteConnectionResponse("Connection request cancelled"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.cancel(req);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Connection request cancelled", resp.getBody().getMessage());
    }

    @Test
    void cancel_serviceThrows_returns500() {
        ConnectionService svc = mock(ConnectionService.class);
        when(svc.cancelInvite(any(), any())).thenThrow(new RuntimeException("boom"));
        ConnectionController controller = new ConnectionController(svc);
        InviteConnectionRequest req = new InviteConnectionRequest(UUID.randomUUID(), UUID.randomUUID());
        var resp = controller.cancel(req);
        assertEquals(500, resp.getStatusCode().value());
    }
}
