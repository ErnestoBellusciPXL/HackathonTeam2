package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.request.*;
import be.codeforbelgium.openinzichten.api.response.AccountCompleteResponse;
import be.codeforbelgium.openinzichten.api.response.AccountResponse;
import be.codeforbelgium.openinzichten.api.response.AuthResponse;
import be.codeforbelgium.openinzichten.api.response.MessageResponse;
import be.codeforbelgium.openinzichten.api.response.AccountStatusResponse;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.security.JwtService;
import be.codeforbelgium.openinzichten.service.AuthService;
import be.codeforbelgium.openinzichten.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, JwtService jwtService, PasswordResetService passwordResetService) {

        this.authService = authService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/me")
    public ResponseEntity<AccountStatusResponse> me(Principal principal) {
        Account account = authService.findByUsername(principal.getName());
        // Return structured status: the frontend will build the human message.
        AccountStatusResponse resp = new AccountStatusResponse(
                account.getId().toString(),
                account.getUsername(),
                account.isDisabled(),
                "",
                account.getDisabledReason(),
                account.getDisabledAt()
        );
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        Account saved = authService.createAccount(req);
        var token = jwtService.generateToken(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRoles(), false);
        var response = new AuthResponse(token);
        return ResponseEntity.created(URI.create("/api/auth/register/" + saved.getId())).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest req) {
        Account account = authService.authenticate(req);

        if (account.isDisabled()) {
            // Only return the reason; frontend composes the full message and contact info.
            MessageResponse resp = new MessageResponse(account.getDisabledReason());
            return ResponseEntity.status(403).body(resp);
        }

        var token = jwtService.generateToken(account.getId(), account.getUsername(), account.getEmail(), account.getRoles(),
                req.getRememberMe());
        var response = new AuthResponse(token);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/complete")
    public ResponseEntity<AccountCompleteResponse> completeRegistration(
            @Valid @RequestBody CompleteRegistrationRequest req, Principal principal) {
        Account updated = authService.completeRegistration(principal.getName(), req);
        List<String> conditionNames = updated.getConditions() == null ? List.of()
                : updated.getConditions().stream().map(c -> c.getName()).sorted().toList();
        List<String> communityNames = updated.getCommunities() == null ? List.of()
                : updated.getCommunities().stream().map(c -> c.getName()).sorted().toList();
        AccountCompleteResponse response = new AccountCompleteResponse(
                updated.getId().toString(),
                updated.getUsername(),
                updated.getEmail(),
                updated.getZipcode(),
                updated.isHasCondition(),
                conditionNames,
                communityNames);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/zipcode")
    public ResponseEntity<AccountResponse> updateZipcode(@Valid @RequestBody CompleteRegistrationRequest req,
                                                         Principal principal) {
        // principal.getName() now comes from JWT authentication filter
        Account updated = authService.updateZipcode(principal.getName(), req.getZipcode());
        AccountResponse response = new AccountResponse(updated.getId().toString(), updated.getUsername(),
                updated.getEmail(), updated.getZipcode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset")
    public ResponseEntity<MessageResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest req) {
        passwordResetService.requestPasswordReset(req);
        MessageResponse response = new MessageResponse(
                "If an account with that email exists, a password reset link has been sent.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        passwordResetService.resetPassword(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check-info")
    public ResponseEntity<Boolean> doesUsernameOrEmailExist(@Valid @RequestBody DoesUserWithUsernameOrEmailExistRequest req) {
        boolean usernameExists = authService.doesUsernameExist(req.getUsername());
        boolean emailExists = authService.doesEmailExist(req.getEmail());
        return ResponseEntity.ok(usernameExists || emailExists);
    }
}
