package be.codeforbelgium.openinzichten.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountDeletedResponse {
    boolean success;
    String message;
}
