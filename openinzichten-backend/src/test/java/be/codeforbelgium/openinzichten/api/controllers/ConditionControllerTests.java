package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.ConditionResponse;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.service.ConditionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionControllerTests {

    private ConditionService conditionService;
    private ConditionController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        conditionService = mock(ConditionService.class);
        controller = new ConditionController(conditionService);
    }

    @Test
    void findAll_whenLimitIsNull_usesZeroAndMapsResults() {
        Condition c = mock(Condition.class);
        when(c.getName()).thenReturn("cond-1");
        when(conditionService.findAll(0)).thenReturn(List.of(c));

        ResponseEntity<List<ConditionResponse>> resp = controller.findAll(null);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        List<ConditionResponse> body = resp.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals("cond-1", body.get(0).name());

        verify(conditionService, times(1)).findAll(0);
    }

    @Test
    void findAll_whenLimitIsNegative_usesZero() {
        when(conditionService.findAll(0)).thenReturn(List.of());

        ResponseEntity<List<ConditionResponse>> resp = controller.findAll(-5);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());

        verify(conditionService, times(1)).findAll(0);
    }

    @Test
    void findAll_whenLimitIsZero_usesZero() {
        when(conditionService.findAll(0)).thenReturn(List.of());

        ResponseEntity<List<ConditionResponse>> resp = controller.findAll(0);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());

        verify(conditionService, times(1)).findAll(0);
    }

    @Test
    void findAll_whenLimitIsPositive_usesValue() {
        Condition c1 = mock(Condition.class);
        when(c1.getName()).thenReturn("a");
        Condition c2 = mock(Condition.class);
        when(c2.getName()).thenReturn("b");

        when(conditionService.findAll(2)).thenReturn(List.of(c1, c2));

        ResponseEntity<List<ConditionResponse>> resp = controller.findAll(2);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        List<ConditionResponse> body = resp.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals("a", body.get(0).name());
        assertEquals("b", body.get(1).name());

        verify(conditionService, times(1)).findAll(2);
    }

    @Test
    void getConditionsByUser_whenAuthenticated_returnsList() {
        // setup security context
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("user1");
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        be.codeforbelgium.openinzichten.domain.Condition c = mock(be.codeforbelgium.openinzichten.domain.Condition.class);
        when(c.getName()).thenReturn("cond-a");
        when(conditionService.getConditionsForUser("user1")).thenReturn(java.util.Set.of(c));

        var resp = controller.getConditionsByUser();
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        var body = resp.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals("cond-a", body.get(0).name());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void getConditionsByUser_whenUnauthenticated_returns401() {
        // ensure no authentication in security context
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        var resp = controller.getConditionsByUser();

        assertNotNull(resp);
        assertEquals(401, resp.getStatusCode().value());
        assertNull(resp.getBody());

        verifyNoInteractions(conditionService);
    }

    @Test
    void getConditionsByUser_whenAuthNameIsNull_returns401() {
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn(null);
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        var resp = controller.getConditionsByUser();

        assertNotNull(resp);
        assertEquals(401, resp.getStatusCode().value());
        assertNull(resp.getBody());

        verifyNoInteractions(conditionService);

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

}
