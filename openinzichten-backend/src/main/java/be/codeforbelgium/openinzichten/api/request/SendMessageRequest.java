package be.codeforbelgium.openinzichten.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull UUID chatId,
        @NotBlank String message) {
}
