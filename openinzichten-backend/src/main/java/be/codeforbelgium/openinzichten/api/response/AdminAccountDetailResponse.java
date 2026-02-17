package be.codeforbelgium.openinzichten.api.response;

import java.util.List;
import java.time.Instant;

public record AdminAccountDetailResponse(
        String id,
        String username,
        String email,
        String postcode,
        boolean disabled,
        List<String> conditions,
        String disabledReason,
        Instant disabledAt
) {
}
