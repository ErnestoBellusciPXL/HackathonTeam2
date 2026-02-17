package be.codeforbelgium.openinzichten.api.response;

import java.util.List;

public record UserAccountResponse(String username, String email, String zipcode, boolean hasCondition, List<String> conditions) {
}