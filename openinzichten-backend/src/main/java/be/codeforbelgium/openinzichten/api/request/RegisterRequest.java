package be.codeforbelgium.openinzichten.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50, message = "Username moet tussen de 3 en 50 karakters zitten")
    @NotBlank(message = "username mag niet leeg zijn")
    private String username;

    @NotBlank(message = "Email mag niet leeg zijn")
    @Email(message = "Dit is geen correct email adres")
    private String email;

    @NotBlank(message = "Password mag niet leeg zijn")
    @Size(min = 8, message = "Password moet meer als 8 karakters bevatten")
    private String password;

}