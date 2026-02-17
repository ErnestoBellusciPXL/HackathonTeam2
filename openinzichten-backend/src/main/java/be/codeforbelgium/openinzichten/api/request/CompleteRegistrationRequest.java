package be.codeforbelgium.openinzichten.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRegistrationRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9\\s-]{3,10}$", message = "invalid_zipcode")
    private String zipcode;

    @NotNull
    private Set<String> conditions;

    private boolean hasCondition;
}