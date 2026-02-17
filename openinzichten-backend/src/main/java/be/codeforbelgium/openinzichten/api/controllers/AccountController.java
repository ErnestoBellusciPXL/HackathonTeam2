package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.request.DeleteAccountRequest;
import be.codeforbelgium.openinzichten.api.request.UpdateAccountRequest;
import be.codeforbelgium.openinzichten.api.response.AccountDeletedResponse;
import be.codeforbelgium.openinzichten.api.response.AccountUpdateResponse;
import be.codeforbelgium.openinzichten.api.response.UserAccountResponse;
import be.codeforbelgium.openinzichten.security.JwtService;
import be.codeforbelgium.openinzichten.service.AccountService;
import be.codeforbelgium.openinzichten.service.UpdateAccountResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService accountService;
    private final JwtService jwtService;

    @DeleteMapping("/delete")
    public ResponseEntity<AccountDeletedResponse> deleteAccount(@Valid @RequestBody DeleteAccountRequest req) {
        UUID userId;
        try {
            userId = UUID.fromString(req.getUserId());
        } catch (IllegalArgumentException e) {
            var response = new AccountDeletedResponse(false, "Invalid user ID format.");
            return ResponseEntity.badRequest().body(response);
        }

        boolean deleted = accountService.deleteAccountById(userId, req.getPassword());

        if (!deleted) {
            var response = new AccountDeletedResponse(false, "Account deletion failed. Incorrect user ID or password.");
            return ResponseEntity.badRequest().body(response);
        }

        var response = new AccountDeletedResponse(true, "Account deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Object> getProfile(HttpServletRequest request) {
        UUID userId = extractUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(401).body("Invalid or missing authentication token.");
        }
        UserAccountResponse account = accountService.getAccountById(userId);
        return ResponseEntity.ok(account);
    }

    @PutMapping
    public ResponseEntity<Object> updateProfile(
            @Valid @RequestBody UpdateAccountRequest req,
            HttpServletRequest request) {
        UUID userId = extractUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(401).body("Invalid or missing authentication token.");
        }

        try {
            UpdateAccountResult result = accountService.updateAccount(userId, req);
            String token = null;
            if (result.usernameChanged() || result.emailChanged()) {
                token = jwtService.generateToken(
                        userId,
                        result.account().username(),
                        result.account().email(),
                        result.roles(),
                        false);
            }
            var response = new AccountUpdateResponse("Instellingen zijn opgeslagen", token, result.account());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private UUID extractUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtService.extractId(token);
    }

}
