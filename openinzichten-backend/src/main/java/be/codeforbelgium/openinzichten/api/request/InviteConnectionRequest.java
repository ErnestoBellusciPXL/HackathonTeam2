package be.codeforbelgium.openinzichten.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InviteConnectionRequest {

    @NotNull(message = "fromUserId mag niet leeg zijn")
    private UUID fromUserId;

    @NotNull(message = "toUserId mag niet leeg zijn")
    private UUID toUserId;
}
