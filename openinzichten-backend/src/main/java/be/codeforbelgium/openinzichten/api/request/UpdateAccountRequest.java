package be.codeforbelgium.openinzichten.api.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateAccountRequest {
    private String username;
    private String email;
    private String zipcode;
    private Boolean hasCondition;
    private Set<String> conditions;
}
