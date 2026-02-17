package be.codeforbelgium.openinzichten.api.request;

import be.codeforbelgium.openinzichten.domain.TicketState;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketStateRequest {

    @NotNull(message = "state must be provided")
    private TicketState state;
}
