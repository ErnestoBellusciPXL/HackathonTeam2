package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.UserAccountResponse;

import java.util.List;

public record UpdateAccountResult(
        UserAccountResponse account,
        boolean usernameChanged,
        boolean emailChanged,
        List<String> roles) {

    public UpdateAccountResult {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
