package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.AdminAccountDetailResponse;
import be.codeforbelgium.openinzichten.api.response.AdminAccountResponse;
import be.codeforbelgium.openinzichten.service.AdminService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class AdminControllerUnitTest {

    @Test
    void getAllAccounts_returnsPage() {
        AdminService svc = Mockito.mock(AdminService.class);
        var controller = new AdminController(svc);

        var page = new PageImpl<AdminAccountResponse>(List.of(new AdminAccountResponse("1", "u", false, false, List.of(), List.of())));
        when(svc.getAllAccounts(anyInt(), anyInt())).thenReturn(page);

        ResponseEntity<Page<AdminAccountResponse>> resp = controller.getAllAccounts(0, 10);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAccountById_returnsDetail() {
        AdminService svc = Mockito.mock(AdminService.class);
        var controller = new AdminController(svc);

        String id = UUID.randomUUID().toString();
        var detail = new AdminAccountDetailResponse(id, "user", "e@e.com", "2000", true, List.of(), "reason", Instant.now());
        when(svc.getAccountById(id)).thenReturn(detail);

        ResponseEntity<AdminAccountDetailResponse> resp = controller.getAccountById(id);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().id()).isEqualTo(id);
        assertThat(resp.getBody().disabledReason()).isEqualTo("reason");
    }

    @Test
    void setAccountDisabled_passesNullReasonWhenMissing() {
        AdminService svc = Mockito.mock(AdminService.class);
        var controller = new AdminController(svc);

        String id = UUID.randomUUID().toString();
        var updated = new AdminAccountDetailResponse(id, "user", "e@e.com", "2000", false, List.of(), null, Instant.now());
        when(svc.setAccountDisabled(id, false, null)).thenReturn(updated);

        Map<String, Object> body = Map.of("disabled", false);
        ResponseEntity<AdminAccountDetailResponse> resp = controller.setAccountDisabled(id, body);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().disabled()).isFalse();
        assertThat(resp.getBody().disabledReason()).isNull();
    }
}
