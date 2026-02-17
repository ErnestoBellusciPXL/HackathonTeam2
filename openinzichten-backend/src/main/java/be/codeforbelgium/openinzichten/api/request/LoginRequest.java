package be.codeforbelgium.openinzichten.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email mag niet leeg zijn")
    private String email;

    @NotBlank(message = "Password mag niet leeg zijn")
    private String password;

    private Boolean rememberMe;
}