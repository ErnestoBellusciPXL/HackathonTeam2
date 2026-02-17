package be.codeforbelgium.openinzichten.api.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DoesUserWithUsernameOrEmailExistRequest {
    private String username;
    private String email;
}
