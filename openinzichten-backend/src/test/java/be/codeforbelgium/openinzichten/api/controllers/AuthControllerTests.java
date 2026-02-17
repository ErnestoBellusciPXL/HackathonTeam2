package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.Community;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.service.AuthService;
import be.codeforbelgium.openinzichten.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private be.codeforbelgium.openinzichten.security.JwtService jwtService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private be.codeforbelgium.openinzichten.service.AccountUserDetailsService accountUserDetailsService;

    @Test
    void register_success() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Account saved = Account.builder()
                .id(id)
                .username("testuser")
                .email("test@example.com")
                .roles(List.of("ROLE_USER"))
                .build();

        when(authService.createAccount(any())).thenReturn(saved);
        when(jwtService.generateToken(id, "testuser", "test@example.com", List.of("ROLE_USER"), false)).thenReturn("tok-123");

        String payload = "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"secret123\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/auth/register/" + id))
                .andExpect(jsonPath("$.token").value("tok-123"));
    }

    @Test
    void login_success() throws Exception {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Account account = Account.builder().id(id).username("loginuser").email("l@example.com").roles(List.of("ROLE_USER")).build();

        when(authService.authenticate(any())).thenReturn(account);
        when(jwtService.generateToken(id, "loginuser", "l@example.com", List.of("ROLE_USER"), true)).thenReturn("login-tok");

        String payload = "{\"username\":\"loginuser\",\"email\":\"l@example.com\",\"password\":\"Password?123\",\"rememberMe\":true}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-tok"));
    }

    @Test
    void register_validationFail_missingPassword() throws Exception {
        String bad = "{\"username\":\"u\",\"email\":\"e@e.com\"}"; // missing password

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeRegistration_success() throws Exception {
        // build account with conditions and communities
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Condition c1 = new Condition();
        c1.setName("Diabetes");
        Condition c2 = new Condition();
        c2.setName("Hypertension");
        Community com1 = new Community();
        com1.setName("CommA");
        Community com2 = new Community();
        com2.setName("CommB");

        Account updated = Account.builder()
                .id(id)
                .username("completeuser")
                .email("c@example.com")
                .zipcode("54321")
                .hasCondition(true)
                .build();
        updated.setConditions(Set.of(c1, c2));
        updated.setCommunities(Set.of(com2, com1));

        when(authService.completeRegistration(eq("principalUser"), any())).thenReturn(updated);

        String payload = "{\"zipcode\":\"54321\",\"conditions\":[\"Diabetes\",\"Hypertension\"],\"hasCondition\":true}";

        mockMvc.perform(put("/api/auth/complete")
                        .principal(() -> "principalUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zipcode").value("54321"))
                .andExpect(jsonPath("$.hasCondition").value(true))
                // conditions are sorted alphabetically by controller
                .andExpect(jsonPath("$.conditions[0]").value("Diabetes"))
                .andExpect(jsonPath("$.conditions[1]").value("Hypertension"))
                // communities sorted
                .andExpect(jsonPath("$.communities[0]").exists());
    }

    @Test
    void updateZipcode_success() throws Exception {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Account updated = Account.builder().id(id).username("zipuser").email("z@example.com").zipcode("99999").build();

        when(authService.updateZipcode("pName", "99999")).thenReturn(updated);

        String payload = "{\"zipcode\":\"99999\",\"conditions\":[],\"hasCondition\":false}";

        mockMvc.perform(put("/api/auth/zipcode")
                        .principal(() -> "pName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zipcode").value("99999"))
                .andExpect(jsonPath("$.username").value("zipuser"));
    }

    @Test
    void passwordReset_endpoints() throws Exception {
        // request
        doNothing().when(passwordResetService).requestPasswordReset(any());

        String req = "{\"email\":\"r@example.com\"}";

        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // confirm
        doNothing().when(passwordResetService).resetPassword(any());
        String conf = "{\"token\":\"tok\",\"newPassword\":\"newpass123\"}";

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conf))
                .andExpect(status().isOk());
    }

    @Test
    void doesUsernameOrEmailExist_both_exist() throws Exception {
        when(authService.doesUsernameExist("existinguser")).thenReturn(true);
        when(authService.doesEmailExist("existing@example.com")).thenReturn(true);

        String payload = "{\"username\":\"existinguser\",\"email\":\"existing@example.com\"}";

        mockMvc.perform(post("/api/auth/check-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void doesUsernameOrEmailExist_only_username_exists() throws Exception {
        when(authService.doesUsernameExist("existinguser")).thenReturn(true);
        when(authService.doesEmailExist("available@example.com")).thenReturn(false);

        String payload = "{\"username\":\"existinguser\",\"email\":\"available@example.com\"}";

        mockMvc.perform(post("/api/auth/check-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void doesUsernameOrEmailExist_only_email_exists() throws Exception {
        when(authService.doesUsernameExist("availableuser")).thenReturn(false);
        when(authService.doesEmailExist("existing@example.com")).thenReturn(true);

        String payload = "{\"username\":\"availableuser\",\"email\":\"existing@example.com\"}";

        mockMvc.perform(post("/api/auth/check-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void doesUsernameOrEmailExist_neither_exists() throws Exception {
        when(authService.doesUsernameExist("availableuser")).thenReturn(false);
        when(authService.doesEmailExist("available@example.com")).thenReturn(false);

        String payload = "{\"username\":\"availableuser\",\"email\":\"available@example.com\"}";

        mockMvc.perform(post("/api/auth/check-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

        @Test
        void me_endpoint_returns_account_status() throws Exception {
                UUID id = UUID.fromString("55555555-5555-5555-5555-555555555555");
                Account a = Account.builder().id(id).username("meuser").email("me@example.com").disabled(true).disabledReason("r").disabledAt(Instant.now()).build();

                when(authService.findByUsername("mePrincipal")).thenReturn(a);

                mockMvc.perform(get("/api/auth/me").principal(() -> "mePrincipal").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.username").value("meuser"));
        }
}
