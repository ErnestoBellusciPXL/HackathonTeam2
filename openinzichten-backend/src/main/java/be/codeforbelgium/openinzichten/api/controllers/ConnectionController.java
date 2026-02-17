package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.request.InviteConnectionRequest;
import be.codeforbelgium.openinzichten.api.response.ConnectionsResponse;
import be.codeforbelgium.openinzichten.api.response.InviteConnectionResponse;
import be.codeforbelgium.openinzichten.service.ConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/connection")
public class ConnectionController {
    private final ConnectionService connectionService;

    @PostMapping("/invite")
    public ResponseEntity<InviteConnectionResponse> invite(@Valid @RequestBody InviteConnectionRequest req) {

        try {
            InviteConnectionResponse response = connectionService.inviteToConnect(req.getFromUserId(),
                    req.getToUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ConnectionsResponse> getConnections(@PathVariable("accountId") UUID accountId) {
        return connectionService.getConnections(accountId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/accept")
    public ResponseEntity<InviteConnectionResponse> accept(@Valid @RequestBody InviteConnectionRequest req) {
        try {
            InviteConnectionResponse response = connectionService.acceptConnection(req.getFromUserId(),
                    req.getToUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<InviteConnectionResponse> reject(@Valid @RequestBody InviteConnectionRequest req) {
        try {
            InviteConnectionResponse response = connectionService.rejectConnection(req.getFromUserId(),
                    req.getToUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/disconnect")
    public ResponseEntity<InviteConnectionResponse> disconnect(@Valid @RequestBody InviteConnectionRequest req) {
        try {
            InviteConnectionResponse response = connectionService.disconnect(req.getFromUserId(), req.getToUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<InviteConnectionResponse> cancel(@Valid @RequestBody InviteConnectionRequest req) {
        try {
            InviteConnectionResponse response = connectionService.cancelInvite(req.getFromUserId(), req.getToUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
