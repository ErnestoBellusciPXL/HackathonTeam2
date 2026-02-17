package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.AdminAccountDetailResponse;
import be.codeforbelgium.openinzichten.config.SecurityConfig;
import be.codeforbelgium.openinzichten.security.JwtService;
import be.codeforbelgium.openinzichten.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTestsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void testSetAccountDisabled_WithoutAuthentication_ShouldReturn401() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/admin/accounts/" + id + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true,\"reason\":\"spam\"}"))
                .andExpect(status().isUnauthorized());

        verify(adminService, never()).setAccountDisabled(any(), any(Boolean.class), any());
    }

    @Test
    @WithMockUser(authorities = "User")
    void testSetAccountDisabled_WithUserRole_ShouldReturn403() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/admin/accounts/" + id + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true,\"reason\":\"spam\"}"))
                .andExpect(status().isForbidden());

        verify(adminService, never()).setAccountDisabled(any(), any(Boolean.class), any());
    }

    @Test
    @WithMockUser(authorities = "Admin")
    void testSetAccountDisabled_WithAdminRole_ShouldPassBodyAndReturnUpdated() throws Exception {
        String id = UUID.randomUUID().toString();
        var updated = new AdminAccountDetailResponse(id, "user", "e@e.com", "2000", true, java.util.List.of(), "spam", Instant.now());
        when(adminService.setAccountDisabled(id, true, "spam")).thenReturn(updated);

        mockMvc.perform(post("/api/admin/accounts/" + id + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true,\"reason\":\"spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disabled").value(true))
                .andExpect(jsonPath("$.disabledReason").value("spam"));

        verify(adminService).setAccountDisabled(id, true, "spam");
    }

    @Test
    @WithMockUser(authorities = "Admin")
    void testSetAccountDisabled_WithAdminRole_MissingReason_ShouldPassNullReason() throws Exception {
        String id = UUID.randomUUID().toString();
        var updated = new AdminAccountDetailResponse(id, "user", "e@e.com", "2000", false, java.util.List.of(), null, Instant.now());
        when(adminService.setAccountDisabled(id, false, null)).thenReturn(updated);

        mockMvc.perform(post("/api/admin/accounts/" + id + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disabled").value(false));

        verify(adminService).setAccountDisabled(id, false, null);
    }

    @Test
    @WithMockUser(authorities = "Admin")
    void testSetAccountDisabled_WithAdminRole_InvalidJson_ShouldReturn400() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/admin/accounts/" + id + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-a-json"))
                .andExpect(status().isBadRequest());

        verify(adminService, never()).setAccountDisabled(any(), any(Boolean.class), any());
    }
}