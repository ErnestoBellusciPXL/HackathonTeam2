package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.request.StoryReformatRequest;
import be.codeforbelgium.openinzichten.api.request.StoryRequest;
import be.codeforbelgium.openinzichten.api.response.StoryReformatResponse;
import be.codeforbelgium.openinzichten.api.response.StoryResponse;
import be.codeforbelgium.openinzichten.domain.Story;
import be.codeforbelgium.openinzichten.security.JwtService;
import be.codeforbelgium.openinzichten.service.AIService;
import be.codeforbelgium.openinzichten.service.StoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryControllerTests {

    private StoryService storyService;
    private AIService aiService;
    private JwtService jwtService;

    @BeforeEach
    void beforeEach() {
        this.storyService = mock(StoryService.class);
        this.aiService = mock(AIService.class);
        this.jwtService = mock(JwtService.class);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addStoryForCurrentUser_unauthenticated_returns401() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        // clear context
        SecurityContextHolder.clearContext();

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void addStoryForCurrentUser_authenticated_callsServiceAndReturnsResponse() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader("Authorization")).thenReturn("Bearer valid-token");

        UUID userId = UUID.randomUUID();
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractId("valid-token")).thenReturn(userId);
        when(storyService.findStoriesByUserId(userId.toString())).thenReturn(List.of());

        Story s = new Story();
        s.setTitle("T");
        s.setContent("C");

        when(storyService.createStoryForUser(eq("me"), any(StoryRequest.class))).thenReturn(s);
        when(storyService.mapToResponse(s, "me")).thenReturn(new StoryResponse(null, "T", "C", List.of("A"), "me", "owner-id-123", true));

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("me", resp.getBody().getOwnerUsername());
        assertEquals("owner-id-123", resp.getBody().getOwnerId());
    }

    @Test
    void addStoryForCurrentUser_serviceThrowsIllegalArgument_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader("Authorization")).thenReturn("Bearer valid-token");

        UUID userId = UUID.randomUUID();
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractId("valid-token")).thenReturn(userId);
        when(storyService.findStoriesByUserId(userId.toString())).thenReturn(List.of());
        when(storyService.createStoryForUser(eq("me"), any(StoryRequest.class))).thenThrow(new IllegalArgumentException("bad request"));

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        assertThrows(IllegalArgumentException.class, () -> controller.addStoryForCurrentUser(req, httpReq));
    }

    @Test
    void addStoryForCurrentUser_missingAuthHeader_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader("Authorization")).thenReturn(null);

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);

        assertEquals(400, resp.getStatusCode().value());
        verifyNoInteractions(storyService);
    }

    @Test
    void addStoryForCurrentUser_invalidAuthHeaderFormat_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader("Authorization")).thenReturn("InvalidFormat token");

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);

        assertEquals(400, resp.getStatusCode().value());
        verifyNoInteractions(storyService);
    }

    @Test
    void addStoryForCurrentUser_invalidToken_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);

        assertEquals(400, resp.getStatusCode().value());
        verifyNoInteractions(storyService);
    }

    @Test
    void addStoryForCurrentUser_tokenWithNullUserId_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractId("valid-token")).thenReturn(null);

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);

        assertEquals(400, resp.getStatusCode().value());
        verifyNoInteractions(storyService);
    }

    @Test
    void addStoryForCurrentUser_userAlreadyHasStories_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader("Authorization")).thenReturn("Bearer valid-token");

        UUID userId = UUID.randomUUID();
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractId("valid-token")).thenReturn(userId);

        StoryResponse existingStory = new StoryResponse("story-1", "Existing", "Content", List.of("A"), "me", userId.toString(), true);
        when(storyService.findStoriesByUserId(userId.toString())).thenReturn(List.of(existingStory));

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);

        assertEquals(400, resp.getStatusCode().value());
        verify(storyService, never()).createStoryForUser(anyString(), any(StoryRequest.class));
    }

    @Test
    void addStoryForCurrentUser_emptyAuthHeader_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader("Authorization")).thenReturn("");

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);

        assertEquals(400, resp.getStatusCode().value());
        verifyNoInteractions(storyService);
    }

    @Test
    void addStoryForCurrentUser_bearerWithoutToken_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getHeader("Authorization")).thenReturn("Bearer ");
        when(jwtService.isTokenValid("")).thenReturn(false);

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);

        assertEquals(400, resp.getStatusCode().value());
        verifyNoInteractions(storyService);
    }

    @Test
    void getStoryById_found_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse sr = new StoryResponse("id-1", "T", "C", List.of("A"), "owner", "owner-1", false);
        when(storyService.findStoryResponseById(eq("id-1"), isNull())).thenReturn(Optional.of(sr));

        var resp = controller.getStoryById("id-1");
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(StoryResponse.class, resp.getBody());
        assertEquals("owner", resp.getBody().getOwnerUsername());
        assertEquals("owner-1", resp.getBody().getOwnerId());
    }

    @Test
    void getStoryById_notFound_returns404() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        when(storyService.findStoryResponseById(eq("missing"), isNull())).thenReturn(Optional.empty());

        var resp = controller.getStoryById("missing");
        assertEquals(404, resp.getStatusCode().value());
        assertNull(resp.getBody());
    }

    @Test
    void getStoryById_invalidId_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        when(storyService.findStoryResponseById(eq("bad-id"), isNull())).thenThrow(new IllegalArgumentException("invalid uuid"));

        assertThrows(IllegalArgumentException.class, () -> controller.getStoryById("bad-id"));
    }

    @Test
    void getStoriesByUserId_success_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse a = new StoryResponse("s1", "T1", "C1", List.of("A"), "u1", (String) null, false);
        StoryResponse b = new StoryResponse("s2", "T2", "C2", List.of("B"), "u1", (String) null, false);

        when(storyService.findStoriesByUserId(eq("u1"), isNull())).thenReturn(List.of(a, b));

        var resp = controller.getStoriesByUserId("u1");
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = resp.getBody();
        assertEquals(2, list.size());
    }

    @Test
    void getStoriesByUserId_invalidId_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        when(storyService.findStoriesByUserId(eq("bad"), isNull())).thenThrow(new IllegalArgumentException("no uuid"));

        assertThrows(IllegalArgumentException.class, () -> controller.getStoriesByUserId("bad"));
    }

    @Test
    void getPagedStories_success_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Title1", "Content1", List.of("A"), "user1", (String) null, false);
        StoryResponse s2 = new StoryResponse("id2", "Title2", "Content2", List.of("B"), "user2", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(s1, s2));

        var resp = controller.getPagedStories(0, 10, null, null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(2, list.size());

        verify(storyService, times(1)).getPagedStories(0, 10, null, null, null, null);
    }

    @Test
    void getPagedStories_emptyList_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        when(storyService.getPagedStories(eq(5), eq(10), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of());

        var resp = controller.getPagedStories(5, 10, null, null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertTrue(list.isEmpty());
    }

    @Test
    void getPagedStories_defaultParameters_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Title1", "Content1", List.of(), "user1", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(s1));

        var resp = controller.getPagedStories(0, 10, null, null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(1, list.size());
    }

    @Test
    void getPagedStories_negativePage_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        var resp = controller.getPagedStories(-1, 10, null, null, null);
        assertEquals(400, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertInstanceOf(Map.class, resp.getBody());

        verifyNoInteractions(storyService);
    }

    @Test
    void getPagedStories_zeroSize_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        var resp = controller.getPagedStories(0, 0, null, null, null);
        assertEquals(400, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertInstanceOf(Map.class, resp.getBody());

        verifyNoInteractions(storyService);
    }

    @Test
    void getPagedStories_negativeSize_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        var resp = controller.getPagedStories(0, -5, null, null, null);
        assertEquals(400, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertInstanceOf(Map.class, resp.getBody());

        verifyNoInteractions(storyService);
    }

    @Test
    void getPagedStories_bothInvalid_returns400() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        var resp = controller.getPagedStories(-1, -1, null, null, null);
        assertEquals(400, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertInstanceOf(Map.class, resp.getBody());

        verifyNoInteractions(storyService);
    }

    @Test
    void getPagedStories_customPageSize_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Title1", "Content1", List.of("A", "B"), "user1", (String) null, false);
        StoryResponse s2 = new StoryResponse("id2", "Title2", "Content2", List.of("C"), "user2", (String) null, false);
        StoryResponse s3 = new StoryResponse("id3", "Title3", "Content3", List.of(), null, (String) null, false);

        when(storyService.getPagedStories(eq(2), eq(3), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(s1, s2, s3));

        var resp = controller.getPagedStories(2, 3, null, null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(3, list.size());

        verify(storyService, times(1)).getPagedStories(2, 3, null, null, null, null);
    }

    @Test
    void getPagedStories_largePageNumber_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        when(storyService.getPagedStories(eq(1000), eq(10), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of());

        var resp = controller.getPagedStories(1000, 10, null, null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertTrue(list.isEmpty());
    }

    @Test
    void getPagedStories_sizeOne_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Title1", "Content1", List.of("A"), "user1", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(1), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(s1));

        var resp = controller.getPagedStories(0, 1, null, null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(1, list.size());

        verify(storyService, times(1)).getPagedStories(0, 1, null, null, null, null);
    }

    @Test
    void getPagedStories_withQueryParam_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Java Tutorial", "Learn Java programming", List.of(), "user1", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), eq("Java"), isNull(), isNull())).thenReturn(List.of(s1));

        var resp = controller.getPagedStories(0, 10, "Java", null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(1, list.size());

        verify(storyService, times(1)).getPagedStories(0, 10, null, "Java", null, null);
    }

    @Test
    void getPagedStories_withEmptyQueryParam_returnsAll() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Title1", "Content1", List.of(), "user1", (String) null, false);
        StoryResponse s2 = new StoryResponse("id2", "Title2", "Content2", List.of(), "user2", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), eq(""), isNull(), isNull())).thenReturn(List.of(s1, s2));

        var resp = controller.getPagedStories(0, 10, "", null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(2, list.size());

        verify(storyService, times(1)).getPagedStories(0, 10, null, "", null, null);
    }

    @Test
    void getPagedStories_withQueryParam_noMatches_returnsEmpty() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), eq("NonExistent"), isNull(), isNull())).thenReturn(List.of());

        var resp = controller.getPagedStories(0, 10, "NonExistent", null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertTrue(list.isEmpty());

        verify(storyService, times(1)).getPagedStories(0, 10, null, "NonExistent", null, null);
    }

    @Test
    void getPagedStories_withQueryParamAndPagination_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Python for beginners", "Content", List.of(), "user1", (String) null, false);

        when(storyService.getPagedStories(eq(1), eq(5), isNull(), eq("Python"), isNull(), isNull())).thenReturn(List.of(s1));

        var resp = controller.getPagedStories(1, 5, "Python", null, null);
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(1, list.size());

        verify(storyService, times(1)).getPagedStories(1, 5, null, "Python", null, null);
    }

    @Test
    void getPagedStories_withOrderByParam_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Title1", "Content1", List.of(), "user1", (String) null, false);
        StoryResponse s2 = new StoryResponse("id2", "Title2", "Content2", List.of(), "user2", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), isNull(), isNull(), eq("createdAt"))).thenReturn(List.of(s1, s2));

        var resp = controller.getPagedStories(0, 10, null, null, "createdAt");
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(2, list.size());

        verify(storyService, times(1)).getPagedStories(0, 10, null, null, null, "createdAt");
    }

    @Test
    void getPagedStories_withOrderByDescending_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Title1", "Content1", List.of(), "user1", (String) null, false);
        StoryResponse s2 = new StoryResponse("id2", "Title2", "Content2", List.of(), "user2", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), isNull(), isNull(), eq("createdAt_desc"))).thenReturn(List.of(s2, s1));

        var resp = controller.getPagedStories(0, 10, null, null, "createdAt_desc");
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(2, list.size());

        verify(storyService, times(1)).getPagedStories(0, 10, null, null, null, "createdAt_desc");
    }

    @Test
    void getPagedStories_withOrderByLikes_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Popular Story", "Content", List.of(), "user1", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), isNull(), isNull(), eq("likes"))).thenReturn(List.of(s1));

        var resp = controller.getPagedStories(0, 10, null, null, "likes");
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(1, list.size());

        verify(storyService, times(1)).getPagedStories(0, 10, null, null, null, "likes");
    }

    @Test
    void getPagedStories_withAllParams_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Java Tutorial", "Learn Java programming", List.of(), "user1", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), eq("Java"), eq("published"), eq("createdAt_desc"))).thenReturn(List.of(s1));

        var resp = controller.getPagedStories(0, 10, "Java", "published", "createdAt_desc");
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(1, list.size());

        verify(storyService, times(1)).getPagedStories(0, 10, null, "Java", "published", "createdAt_desc");
    }

    @Test
    void getPagedStories_withEmptyOrderBy_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Title1", "Content1", List.of(), "user1", (String) null, false);

        when(storyService.getPagedStories(eq(0), eq(10), isNull(), isNull(), isNull(), eq(""))).thenReturn(List.of(s1));

        var resp = controller.getPagedStories(0, 10, null, null, "");
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(1, list.size());

        verify(storyService, times(1)).getPagedStories(0, 10, null, null, null, "");
    }

    @Test
    void getPagedStories_withOrderByAndPagination_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        StoryResponse s1 = new StoryResponse("id1", "Title1", "Content1", List.of(), "user1", (String) null, false);
        StoryResponse s2 = new StoryResponse("id2", "Title2", "Content2", List.of(), "user2", (String) null, false);
        StoryResponse s3 = new StoryResponse("id3", "Title3", "Content3", List.of(), "user3", (String) null, false);

        when(storyService.getPagedStories(eq(1), eq(3), isNull(), isNull(), isNull(), eq("title"))).thenReturn(List.of(s1, s2, s3));

        var resp = controller.getPagedStories(1, 3, null, null, "title");
        assertEquals(200, resp.getStatusCode().value());
        assertInstanceOf(List.class, resp.getBody());
        List<?> list = (List<?>) resp.getBody();
        assertEquals(3, list.size());

        verify(storyService, times(1)).getPagedStories(1, 3, null, null, null, "title");
    }

    @Test
    void likeStory_unauthenticated_returns401() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        SecurityContextHolder.clearContext();

        var resp = controller.likeStory("story-id");
        assertEquals(401, resp.getStatusCode().value());
        verifyNoInteractions(storyService);
    }

    @Test
    void likeStory_authenticated_callsService() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        StoryResponse response = new StoryResponse("story-id", "Title", "Content", List.of("A"), "owner", new StoryResponse.LikeInfo(1L, true), false);
        when(storyService.likeStory("me", "story-id")).thenReturn(response);

        var resp = controller.likeStory("story-id");
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(response, resp.getBody());
        verify(storyService).likeStory("me", "story-id");
    }

    @Test
    void unlikeStory_unauthenticated_returns401() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        SecurityContextHolder.clearContext();

        var resp = controller.unlikeStory("story-id");
        assertEquals(401, resp.getStatusCode().value());
        verifyNoInteractions(storyService);
    }

    @Test
    void unlikeStory_authenticated_callsService() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        StoryResponse response = new StoryResponse("story-id", "Title", "Content", List.of("A"), "owner", "OL", false);
        when(storyService.unlikeStory("me", "story-id")).thenReturn(response);

        var resp = controller.unlikeStory("story-id");
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(response, resp.getBody());
        verify(storyService).unlikeStory("me", "story-id");
    }

    // Tests for /ai/reformat endpoint

    @Test
    void reformatStory_validRequest_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        String originalContent = "This is my story about healthcare access.";
        String reformattedContent = "This is a reformatted story about healthcare access with better structure.";

        when(aiService.doStoryReformatting(originalContent)).thenReturn(reformattedContent);

        StoryReformatRequest request = new StoryReformatRequest(originalContent);
        var resp = controller.reformatStory(request);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertInstanceOf(StoryReformatResponse.class, resp.getBody());
        StoryReformatResponse response = (StoryReformatResponse) resp.getBody();
        assertEquals(reformattedContent, response.getReformattedStoryContent());

        verify(aiService, times(1)).doStoryReformatting(originalContent);
    }

    @Test
    void reformatStory_longContent_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        String longContent = "A".repeat(5000);
        String reformattedContent = "Reformatted long content.";

        when(aiService.doStoryReformatting(longContent)).thenReturn(reformattedContent);

        StoryReformatRequest request = new StoryReformatRequest(longContent);
        var resp = controller.reformatStory(request);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        StoryReformatResponse response = (StoryReformatResponse) resp.getBody();
        assertEquals(reformattedContent, response.getReformattedStoryContent());

        verify(aiService, times(1)).doStoryReformatting(longContent);
    }

    @Test
    void reformatStory_specialCharacters_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        String contentWithSpecialChars = "Story with special chars: <>&\"'@#$%^*()[]{}|\\";
        String reformattedContent = "Reformatted story with special characters.";

        when(aiService.doStoryReformatting(contentWithSpecialChars)).thenReturn(reformattedContent);

        StoryReformatRequest request = new StoryReformatRequest(contentWithSpecialChars);
        var resp = controller.reformatStory(request);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        StoryReformatResponse response = (StoryReformatResponse) resp.getBody();
        assertEquals(reformattedContent, response.getReformattedStoryContent());

        verify(aiService, times(1)).doStoryReformatting(contentWithSpecialChars);
    }

    @Test
    void reformatStory_unicodeContent_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        String unicodeContent = "Story with émojis 😊 and spëcial çharacters";
        String reformattedContent = "Reformatted story with unicode.";

        when(aiService.doStoryReformatting(unicodeContent)).thenReturn(reformattedContent);

        StoryReformatRequest request = new StoryReformatRequest(unicodeContent);
        var resp = controller.reformatStory(request);

        assertEquals(200, resp.getStatusCode().value());
        StoryReformatResponse response = (StoryReformatResponse) resp.getBody();
        assertEquals(reformattedContent, response.getReformattedStoryContent());
    }

    @Test
    void reformatStory_emptyReformattedContent_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        String originalContent = "Some content";
        String emptyReformatted = "";

        when(aiService.doStoryReformatting(originalContent)).thenReturn(emptyReformatted);

        StoryReformatRequest request = new StoryReformatRequest(originalContent);
        var resp = controller.reformatStory(request);

        assertEquals(200, resp.getStatusCode().value());
        StoryReformatResponse response = (StoryReformatResponse) resp.getBody();
        assertEquals(emptyReformatted, response.getReformattedStoryContent());
    }

    @Test
    void reformatStory_aiServiceThrowsException_propagatesException() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        String content = "Test content";
        when(aiService.doStoryReformatting(content)).thenThrow(new RuntimeException("AI service error"));

        StoryReformatRequest request = new StoryReformatRequest(content);

        assertThrows(RuntimeException.class, () -> controller.reformatStory(request));
        verify(aiService, times(1)).doStoryReformatting(content);
    }

    @Test
    void reformatStory_multilineContent_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        String multilineContent = "Line 1\nLine 2\nLine 3\nLine 4";
        String reformattedContent = "Reformatted multiline story.";

        when(aiService.doStoryReformatting(multilineContent)).thenReturn(reformattedContent);

        StoryReformatRequest request = new StoryReformatRequest(multilineContent);
        var resp = controller.reformatStory(request);

        assertEquals(200, resp.getStatusCode().value());
        StoryReformatResponse response = (StoryReformatResponse) resp.getBody();
        assertEquals(reformattedContent, response.getReformattedStoryContent());
    }

    // Tests for updateStoryForCurrentUser endpoint

    @Test
    void updateStoryForCurrentUser_unauthenticated_returns401() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        // clear context
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        UUID.randomUUID(), "Title", "Content", List.of("A"));
        var resp = controller.updateStoryForCurrentUser(req);
        assertEquals(401, resp.getStatusCode().value());

        verifyNoInteractions(storyService);
    }

    @Test
    void updateStoryForCurrentUser_authenticationNull_returns401() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(null);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        UUID.randomUUID(), "Title", "Content", List.of("A"));
        var resp = controller.updateStoryForCurrentUser(req);
        assertEquals(401, resp.getStatusCode().value());

        verifyNoInteractions(storyService);
    }

    @Test
    void updateStoryForCurrentUser_authenticationNameNull_returns401() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn(null);
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        UUID.randomUUID(), "Title", "Content", List.of("A"));
        var resp = controller.updateStoryForCurrentUser(req);
        assertEquals(401, resp.getStatusCode().value());

        verifyNoInteractions(storyService);
    }

    @Test
    void updateStoryForCurrentUser_authenticated_callsServiceAndReturnsResponse() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UUID storyId = UUID.randomUUID();
        Story updatedStory = new Story();
        updatedStory.setId(storyId);
        updatedStory.setTitle("Updated Title");
        updatedStory.setContent("Updated Content");

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        storyId, "Updated Title", "Updated Content", List.of("Condition1"));

        when(storyService.updateStoryForUser(eq("testuser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class)))
                .thenReturn(updatedStory);
        when(storyService.mapToResponse(updatedStory)).thenReturn(
                new StoryResponse(storyId.toString(), "Updated Title", "Updated Content",
                        List.of("Condition1"), "testuser", "user-id", false));

        var resp = controller.updateStoryForCurrentUser(req);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("Updated Title", resp.getBody().getTitle());
        assertEquals("Updated Content", resp.getBody().getContent());
        assertEquals("testuser", resp.getBody().getOwnerUsername());

        verify(storyService, times(1)).updateStoryForUser(eq("testuser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class));
        verify(storyService, times(1)).mapToResponse(updatedStory);
    }

    @Test
    void updateStoryForCurrentUser_serviceThrowsIllegalArgumentException_propagatesException() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        UUID.randomUUID(), "Title", "Content", List.of("InvalidCondition"));

        when(storyService.updateStoryForUser(eq("testuser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("Condition(s) not linked to account: InvalidCondition"));

        assertThrows(IllegalArgumentException.class, () -> controller.updateStoryForCurrentUser(req));

        verify(storyService, times(1)).updateStoryForUser(eq("testuser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class));
    }

    @Test
    void updateStoryForCurrentUser_emptyContent_serviceThrowsException() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        UUID.randomUUID(), "Title", "", List.of("A"));

        when(storyService.updateStoryForUser(eq("testuser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("Story content must not be empty"));

        assertThrows(IllegalArgumentException.class, () -> controller.updateStoryForCurrentUser(req));
    }

    @Test
    void updateStoryForCurrentUser_multipleConditions_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UUID storyId = UUID.randomUUID();
        Story updatedStory = new Story();
        updatedStory.setId(storyId);
        updatedStory.setTitle("Story with Multiple Conditions");
        updatedStory.setContent("Content");

        List<String> conditions = List.of("Condition1", "Condition2", "Condition3");
        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        storyId, "Story with Multiple Conditions", "Content", conditions);

        when(storyService.updateStoryForUser(eq("testuser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class)))
                .thenReturn(updatedStory);
        when(storyService.mapToResponse(updatedStory)).thenReturn(
                new StoryResponse(storyId.toString(), "Story with Multiple Conditions", "Content",
                        conditions, "testuser", "user-id", false));

        var resp = controller.updateStoryForCurrentUser(req);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(3, resp.getBody().getConditionNames().size());
    }

    @Test
    void updateStoryForCurrentUser_longContent_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UUID storyId = UUID.randomUUID();
        String longContent = "A".repeat(5000);
        Story updatedStory = new Story();
        updatedStory.setId(storyId);
        updatedStory.setTitle("Long Story");
        updatedStory.setContent(longContent);

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        storyId, "Long Story", longContent, List.of("Condition1"));

        when(storyService.updateStoryForUser(eq("testuser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class)))
                .thenReturn(updatedStory);
        when(storyService.mapToResponse(updatedStory)).thenReturn(
                new StoryResponse(storyId.toString(), "Long Story", longContent,
                        List.of("Condition1"), "testuser", "user-id", false));

        var resp = controller.updateStoryForCurrentUser(req);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(longContent, resp.getBody().getContent());
    }

    @Test
    void updateStoryForCurrentUser_specialCharacters_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UUID storyId = UUID.randomUUID();
        String specialContent = "Story with special chars: <>&\"'@#$%^*()[]{}|\\";
        Story updatedStory = new Story();
        updatedStory.setId(storyId);
        updatedStory.setTitle("Special Title");
        updatedStory.setContent(specialContent);

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        storyId, "Special Title", specialContent, List.of("Condition1"));

        when(storyService.updateStoryForUser(eq("testuser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class)))
                .thenReturn(updatedStory);
        when(storyService.mapToResponse(updatedStory)).thenReturn(
                new StoryResponse(storyId.toString(), "Special Title", specialContent,
                        List.of("Condition1"), "testuser", "user-id", false));

        var resp = controller.updateStoryForCurrentUser(req);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(specialContent, resp.getBody().getContent());
    }

    @Test
    void updateStoryForCurrentUser_unicodeContent_returns200() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UUID storyId = UUID.randomUUID();
        String unicodeContent = "Story with émojis 😊 and spëcial çharacters";
        Story updatedStory = new Story();
        updatedStory.setId(storyId);
        updatedStory.setTitle("Unicode Title");
        updatedStory.setContent(unicodeContent);

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        storyId, "Unicode Title", unicodeContent, List.of("Condition1"));

        when(storyService.updateStoryForUser(eq("testuser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class)))
                .thenReturn(updatedStory);
        when(storyService.mapToResponse(updatedStory)).thenReturn(
                new StoryResponse(storyId.toString(), "Unicode Title", unicodeContent,
                        List.of("Condition1"), "testuser", "user-id", false));

        var resp = controller.updateStoryForCurrentUser(req);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(unicodeContent, resp.getBody().getContent());
    }

    @Test
    void updateStoryForCurrentUser_differentUser_updatesWithCorrectUsername() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("anotheruser");
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        UUID storyId = UUID.randomUUID();
        Story updatedStory = new Story();
        updatedStory.setId(storyId);
        updatedStory.setTitle("Title");
        updatedStory.setContent("Content");

        be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest req =
                new be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest(
                        storyId, "Title", "Content", List.of("Condition1"));

        when(storyService.updateStoryForUser(eq("anotheruser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class)))
                .thenReturn(updatedStory);
        when(storyService.mapToResponse(updatedStory)).thenReturn(
                new StoryResponse(storyId.toString(), "Title", "Content",
                        List.of("Condition1"), "anotheruser", "another-user-id", false));

        var resp = controller.updateStoryForCurrentUser(req);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("anotheruser", resp.getBody().getOwnerUsername());

        verify(storyService, times(1)).updateStoryForUser(eq("anotheruser"), any(be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest.class));
    }

    @Test
    void deleteStory_unauthenticated_returns401() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        // ensure no auth
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        var resp = controller.deleteStory("story-id");
        assertEquals(401, resp.getStatusCode().value());

        verifyNoInteractions(storyService);
    }

    @Test
    void deleteStory_authenticated_callsService_andReturnsNoContent() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("me");
        when(auth.isAuthenticated()).thenReturn(true);
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        // storyService.deleteStoryForUser is void; just verify it's called
        var resp = controller.deleteStory("story-id");

        assertEquals(204, resp.getStatusCode().value());
        verify(storyService, times(1)).deleteStoryForUser("me", "story-id");
    }

    @Test
    void getAuthenticatedUsername_whenIsAuthenticatedFalse_returnsEmpty() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);
        assertEquals(401, resp.getStatusCode().value());

        verifyNoInteractions(storyService);
    }


    @Test
    void getAuthenticatedUsername_whenAnonymousAuthenticationToken_returnsEmpty() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        org.springframework.security.authentication.AnonymousAuthenticationToken anon =
                new org.springframework.security.authentication.AnonymousAuthenticationToken(
                        "key",
                        "anonymousUser",
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        org.springframework.security.core.context.SecurityContext sc = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(anon);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp = controller.addStoryForCurrentUser(req, httpReq);
        assertEquals(401, resp.getStatusCode().value());

        verifyNoInteractions(storyService);
    }

    @Test
    void getAuthenticatedUsername_whenNameBlankOrAnonymousUser_returnsEmpty() {
        StoryController controller = new StoryController(storyService, aiService, jwtService);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);

        // blank name
        org.springframework.security.core.Authentication authBlank = mock(org.springframework.security.core.Authentication.class);
        when(authBlank.getName()).thenReturn("   ");
        when(authBlank.isAuthenticated()).thenReturn(true);
        org.springframework.security.core.context.SecurityContext sc1 = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc1.getAuthentication()).thenReturn(authBlank);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc1);

        StoryRequest req = new StoryRequest("T", "C", List.of("A"));
        var resp1 = controller.addStoryForCurrentUser(req, httpReq);
        assertEquals(401, resp1.getStatusCode().value());
        verifyNoInteractions(storyService);

        // name equals literal anonymousUser
        org.springframework.security.core.Authentication authAnonName = mock(org.springframework.security.core.Authentication.class);
        when(authAnonName.getName()).thenReturn("anonymousUser");
        when(authAnonName.isAuthenticated()).thenReturn(true);
        org.springframework.security.core.context.SecurityContext sc2 = mock(org.springframework.security.core.context.SecurityContext.class);
        when(sc2.getAuthentication()).thenReturn(authAnonName);
        org.springframework.security.core.context.SecurityContextHolder.setContext(sc2);

        var resp2 = controller.addStoryForCurrentUser(req, httpReq);
        assertEquals(401, resp2.getStatusCode().value());
        verifyNoInteractions(storyService);
    }
}


