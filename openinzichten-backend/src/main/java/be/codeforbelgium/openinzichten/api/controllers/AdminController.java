package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.AdminAccountResponse;
import be.codeforbelgium.openinzichten.api.response.AdminAccountDetailResponse;
import be.codeforbelgium.openinzichten.service.AdminService;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    @GetMapping(value = "/accounts", produces = "application/json")
    public ResponseEntity<Page<AdminAccountResponse>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<AdminAccountResponse> accounts = adminService.getAllAccounts(page, pageSize);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(accounts);
    }

    @GetMapping(value = "/accounts/{id}", produces = "application/json")
    public ResponseEntity<AdminAccountDetailResponse> getAccountById(@PathVariable String id) {
        AdminAccountDetailResponse resp = adminService.getAccountById(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp);
    }

    @PostMapping("/accounts/{id}/disable")
    public ResponseEntity<AdminAccountDetailResponse> setAccountDisabled(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {

        Boolean disabled = body.get("disabled") == null ? null : (Boolean) body.get("disabled");
        String reason = (String) body.get("reason");

        AdminAccountDetailResponse updated = adminService.setAccountDisabled(id, disabled, reason);
        return ResponseEntity.ok(updated);
    }
}
