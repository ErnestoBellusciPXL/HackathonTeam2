package be.codeforbelgium.openinzichten.api.response;

public record AccountUpdateResponse(String message, String token, UserAccountResponse account) {
}
