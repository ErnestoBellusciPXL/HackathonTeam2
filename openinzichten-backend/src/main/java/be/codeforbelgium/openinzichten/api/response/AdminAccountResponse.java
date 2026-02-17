package be.codeforbelgium.openinzichten.api.response;

import java.util.List;

public record AdminAccountResponse(String id, String username, boolean disabled, boolean hasCondition, List<String> conditions,
                                   List<String> communities) {
}
