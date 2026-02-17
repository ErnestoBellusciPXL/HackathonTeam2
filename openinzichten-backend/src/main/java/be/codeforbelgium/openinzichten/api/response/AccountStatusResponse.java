package be.codeforbelgium.openinzichten.api.response;

import java.time.Instant;

public record AccountStatusResponse(String id, String username, boolean disabled, String message, String disabledReason, Instant disabledAt) {
}
