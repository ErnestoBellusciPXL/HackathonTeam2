package be.codeforbelgium.openinzichten.api.response;

import java.util.List;
import java.util.UUID;


public record ConnectionAccountResponse(String id,
                                        String username,
                                        boolean hasCondition,
                                        List<String> conditions,
                                        UUID chatId) {
}
