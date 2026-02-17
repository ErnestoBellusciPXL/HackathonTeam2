package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.UserAccountResponse;
import be.codeforbelgium.openinzichten.service.AccountService;
import be.codeforbelgium.openinzichten.service.UpdateAccountResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private be.codeforbelgium.openinzichten.security.JwtService jwtService;

    @MockitoBean
    private be.codeforbelgium.openinzichten.service.AccountUserDetailsService accountUserDetailsService;

    @Test
    void deleteAccount_success() throws Exception {
        String id = "11111111-1111-1111-1111-111111111111";
        String password = "secret";

        when(accountService.deleteAccountById(java.util.UUID.fromString(id), password)).thenReturn(true);

        String payload = "{\"userId\":\"" + id + "\",\"password\":\"" + password + "\"}";

        mockMvc.perform(delete("/api/account/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));
    }

    @Test
    void deleteAccount_failure() throws Exception {
        String id = "22222222-2222-2222-2222-222222222222";
        String password = "badpw";

        when(accountService.deleteAccountById(java.util.UUID.fromString(id), password)).thenReturn(false);

        String payload = "{\"userId\":\"" + id + "\",\"password\":\"" + password + "\"}";

        mockMvc.perform(delete("/api/account/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteAccount_invalidUuid_returnsBadRequest() throws Exception {
        String invalidId = "not-a-uuid";
        String password = "pw";

        String payload = "{\"userId\":\"" + invalidId + "\",\"password\":\"" + password + "\"}";

        mockMvc.perform(delete("/api/account/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid user ID format."));
    }

    @Test
    void updateProfile_success_with_valid_token() throws Exception {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        String token = "valid.jwt.token";
        String newToken = "new.jwt.token";
        
        when(jwtService.extractId(token)).thenReturn(userId);
        when(accountService.updateAccount(eq(userId), any())).thenReturn(
                new UpdateAccountResult(
                        new UserAccountResponse("newUsername", "new@email.com", "1000", false, List.of()),
                        true,
                        false,
                        List.of("User")
                )
        );
        when(jwtService.generateToken(userId, "newUsername", "new@email.com", List.of("User"), false))
                .thenReturn(newToken);

        String payload = "{\"username\":\"newUsername\",\"email\":\"new@email.com\"}";

        mockMvc.perform(put("/api/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Instellingen zijn opgeslagen"))
                .andExpect(jsonPath("$.token").value(newToken))
                .andExpect(jsonPath("$.account.username").value("newUsername"))
                .andExpect(jsonPath("$.account.email").value("new@email.com"));
    }

    @Test
    void updateProfile_success_without_identity_change_returns_null_token() throws Exception {
        UUID userId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        String token = "valid.jwt.token";

        when(jwtService.extractId(token)).thenReturn(userId);
        when(accountService.updateAccount(eq(userId), any())).thenReturn(
                new UpdateAccountResult(
                        new UserAccountResponse("sameUser", "same@email.com", "1000", false, List.of()),
                        false,
                        false,
                        List.of("User")
                )
        );

        String payload = "{\"conditions\":[\"ExistingCondition\"]}";

        mockMvc.perform(put("/api/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Instellingen zijn opgeslagen"))
                .andExpect(jsonPath("$.token").value(nullValue()))
                .andExpect(jsonPath("$.account.username").value("sameUser"));

        verify(jwtService, never()).generateToken(any(UUID.class), anyString(), anyString(), anyList(), anyBoolean());
    }

    @Test
    void updateProfile_unauthorized_when_no_token() throws Exception {
        String payload = "{\"username\":\"newUsername\",\"email\":\"new@email.com\"}";

        mockMvc.perform(put("/api/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_unauthorized_when_invalid_token() throws Exception {
        String token = "invalid.jwt.token";
        
        when(jwtService.extractId(token)).thenReturn(null);

        String payload = "{\"username\":\"newUsername\",\"email\":\"new@email.com\"}";

        mockMvc.perform(put("/api/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_badRequest_when_username_taken() throws Exception {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        String token = "valid.jwt.token";
        
        when(jwtService.extractId(token)).thenReturn(userId);
        when(accountService.updateAccount(eq(userId), any()))
            .thenThrow(new IllegalArgumentException("Username already taken"));

        String payload = "{\"username\":\"takenUsername\",\"email\":\"new@email.com\"}";

        mockMvc.perform(put("/api/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProfile_success_with_valid_token() throws Exception {
        UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        String token = "valid.jwt.token";
        
        when(jwtService.extractId(token)).thenReturn(userId);
        when(accountService.getAccountById(userId)).thenReturn(
            new UserAccountResponse("testUser", "test@email.com", "1000", true, List.of("Depression"))
        );

        mockMvc.perform(get("/api/account")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.zipcode").value("1000"));
    }

    @Test
    void getProfile_unauthorized_when_no_token() throws Exception {
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized());
    }
}
