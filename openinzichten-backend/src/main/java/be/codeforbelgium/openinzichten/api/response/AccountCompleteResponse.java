package be.codeforbelgium.openinzichten.api.response;

import java.util.List;

public record AccountCompleteResponse(String id, String username, String email, String zipcode, boolean hasCondition,
                                      List<String> conditions, List<String> communities) {
}
