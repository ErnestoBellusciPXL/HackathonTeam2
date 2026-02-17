package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.request.StoryRequest;
import be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest;
import be.codeforbelgium.openinzichten.api.response.StoryResponse;
import be.codeforbelgium.openinzichten.domain.*;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.StoryLikeRepository;
import be.codeforbelgium.openinzichten.repository.StoryRepository;
import be.codeforbelgium.openinzichten.repository.TicketRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StoryServiceTests {

    private StoryService createService(StoryRepository storyRepo, AccountRepository accRepo) {
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        TicketRepository ticketRepo = mock(TicketRepository.class);
        return new StoryService(storyRepo, accRepo, likeRepo, ticketRepo);
    }

    private StoryService createService(StoryRepository storyRepo, AccountRepository accRepo, StoryLikeRepository likeRepo) {
        TicketRepository ticketRepo = mock(TicketRepository.class);
        return new StoryService(storyRepo, accRepo, likeRepo, ticketRepo);
    }

    private StoryService createService(StoryRepository storyRepo, AccountRepository accRepo, StoryLikeRepository likeRepo, TicketRepository ticketRepo) {
        return new StoryService(storyRepo, accRepo, likeRepo, ticketRepo);
    }

    @Test
    void createStoryForUser_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("alice")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("T", "Hello world", List.of("C1"));

        when(storyRepo.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));

        Story created = svc.createStoryForUser("alice", req);

        assertNotNull(created);
        assertEquals("T", created.getTitle());
        assertEquals("Hello world", created.getContent());
        assertEquals(1, created.getConditions().size());
        assertEquals(a, created.getOwner());

        verify(storyRepo, times(1)).save(any(Story.class));
    }

    @Test
    void createStoryForUser_conditionNotLinked_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("bob")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("T2", "Text", List.of("NOT_LINKED"));

        assertThrows(IllegalArgumentException.class, () -> svc.createStoryForUser("bob", req));
        verifyNoInteractions(storyRepo);
    }

    @Test
    void createStoryForUser_emptyContent_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("carol")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("T", "   ", List.of("C1"));

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.createStoryForUser("carol", req));
        assertEquals("Story content must not be empty", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void createStoryForUser_noConditions_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        a.setConditions(java.util.Set.of());
        when(accRepo.findWithConditionsByUsername("dave")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("T", "content", java.util.List.of());

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.createStoryForUser("dave", req));
        assertEquals("At least one condition must be specified", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void createStoryForUser_accountNotFound_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        when(accRepo.findWithConditionsByUsername("missing")).thenReturn(java.util.Optional.empty());

        StoryRequest req = new StoryRequest("T", "content", List.of("C1"));

        assertThrows(be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException.class,
                () -> svc.createStoryForUser("missing", req));
        verifyNoInteractions(storyRepo);
    }

    @Test
    void mapToResponse_and_findStoryResponseById_and_getStoryById_and_findStoriesByUserId() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        UUID ownerId = java.util.UUID.randomUUID();
        owner.setId(ownerId);

        Condition cond = new Condition();
        cond.setName("COND");

        Story s = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Title")
                .content("Some content")
                .owner(owner)
                .conditions(java.util.Set.of(cond))
                .build();

        // repository behaviors
        when(storyRepo.findById(s.getId())).thenReturn(java.util.Optional.of(s));
        when(storyRepo.findByOwnerId(s.getOwner().getId())).thenReturn(java.util.List.of(s));

        // mapToResponse via findStoryResponseById
        var opt = svc.findStoryResponseById(s.getId().toString());
        assertTrue(opt.isPresent());
        var resp = opt.get();
        assertEquals(s.getTitle(), resp.getTitle());
        assertEquals("owner1", resp.getOwnerUsername());
        assertEquals(ownerId.toString(), resp.getOwnerId());
        assertEquals(1, resp.getConditionNames().size());

        // getStoryById
        var maybe = svc.getStoryById(s.getId());
        assertTrue(maybe.isPresent());
        assertEquals(s.getTitle(), maybe.get().getTitle());

        // findStoriesByUserId success
        UUID ownerUuid = s.getOwner().getId();
        when(storyRepo.findByOwnerId(ownerUuid)).thenReturn(java.util.List.of(s));

        var list = svc.findStoriesByUserId(ownerUuid.toString());
        assertEquals(1, list.size());
        assertEquals(s.getTitle(), list.get(0).getTitle());
        assertEquals(ownerId.toString(), list.get(0).getOwnerId());
    }

    @Test
    void findStoriesByUserId_invalidUuid_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        assertThrows(IllegalArgumentException.class, () -> svc.findStoriesByUserId("not-a-uuid"));
    }

    @Test
    void findStoryResponseById_invalidUuid_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        assertThrows(IllegalArgumentException.class, () -> svc.findStoryResponseById("no-uuid"));
    }

    @Test
    void mapToResponse_allFieldsPresent() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("ownerX");
        UUID ownerId = java.util.UUID.randomUUID();
        owner.setId(ownerId);

        Condition cond = new Condition();
        cond.setName("COND1");

        Story s = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("MyTitle")
                .content("Some content")
                .owner(owner)
                .conditions(java.util.Set.of(cond))
                .build();

        var resp = svc.mapToResponse(s);
        assertNotNull(resp);
        assertEquals(s.getId().toString(), resp.getId());
        assertEquals("MyTitle", resp.getTitle());
        assertEquals("Some content", resp.getContent());
        assertEquals(1, resp.getConditionNames().size());
        assertEquals("COND1", resp.getConditionNames().get(0));
        assertEquals("ownerX", resp.getOwnerUsername());
        assertEquals(ownerId.toString(), resp.getOwnerId());
    }

    @Test
    void mapToResponse_handlesNulls() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        // story with null id, null conditions and null owner
        Story s = Story.builder()
                .id(null)
                .title(null)
                .content(null)
                .owner(null)
                .conditions(null)
                .build();

        var resp = svc.mapToResponse(s);
        assertNotNull(resp);
        assertNull(resp.getId());
        assertNull(resp.getTitle());
        assertNull(resp.getContent());
        assertNotNull(resp.getConditionNames());
        assertTrue(resp.getConditionNames().isEmpty());
        assertNull(resp.getOwnerUsername());
        assertNull(resp.getOwnerId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("owner1");

        Condition cond = new Condition();
        cond.setName("COND1");

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 1")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of(cond))
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 2")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of(cond))
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1, s2));
        when(storyRepo.searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Story 1", result.get(0).getTitle());
        assertEquals("Story 2", result.get(1).getTitle());
        assertEquals("owner1", result.get(0).getOwnerUsername());
        assertEquals("COND1", result.get(0).getConditionNames().get(0));

        verify(storyRepo, times(1)).searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_withQueryAndConditionsAndOrderBy_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("owner1");

        Condition cond = new Condition();
        cond.setName("COND1");

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 1")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of(cond))
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1));
        when(storyRepo.searchByQueryAndConditionNames(eq("test"), eq(List.of("COND1")), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "test", "COND1", "");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Story 1", result.get(0).getTitle());
        verify(storyRepo, times(1)).searchByQueryAndConditionNames(eq("test"), eq(List.of("COND1")), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_emptyPage() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.empty());
        when(storyRepo.searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(storyRepo, times(1)).searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_withDifferentPageSizes() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("owner1");

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 1")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1));
        when(storyRepo.searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(2, 5);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(storyRepo, times(1)).searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void mapStoryToResponse_allFieldsPresent() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("testUser");
        UUID ownerId = java.util.UUID.randomUUID();
        owner.setId(ownerId);

        Condition cond1 = new Condition();
        cond1.setName("Condition1");
        Condition cond2 = new Condition();
        cond2.setName("Condition2");

        UUID storyId = java.util.UUID.randomUUID();
        Story s = Story.builder()
                .id(storyId)
                .title("Test Title")
                .content("Test Content")
                .owner(owner)
                .conditions(java.util.Set.of(cond1, cond2))
                .build();

        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(s));

        var resp = svc.findStoryResponseById(storyId.toString());
        assertTrue(resp.isPresent());
        var storyResp = resp.get();

        assertEquals(storyId.toString(), storyResp.getId());
        assertEquals("Test Title", storyResp.getTitle());
        assertEquals("Test Content", storyResp.getContent());
        assertEquals(2, storyResp.getConditionNames().size());
        assertTrue(storyResp.getConditionNames().contains("Condition1"));
        assertTrue(storyResp.getConditionNames().contains("Condition2"));
        assertEquals("testUser", storyResp.getOwnerUsername());
        assertEquals(ownerId.toString(), storyResp.getOwnerId());
    }

    @Test
    void mapStoryToResponse_nullConditions() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("testUser");

        UUID storyId = java.util.UUID.randomUUID();
        Story s = Story.builder()
                .id(storyId)
                .title("Test Title")
                .content("Test Content")
                .owner(owner)
                .conditions(null)
                .build();

        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(s));

        var resp = svc.findStoryResponseById(storyId.toString());
        assertTrue(resp.isPresent());
        var storyResp = resp.get();

        assertNotNull(storyResp.getConditionNames());
        assertTrue(storyResp.getConditionNames().isEmpty());
    }

    @Test
    void mapStoryToResponse_emptyConditions() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("testUser");

        UUID storyId = java.util.UUID.randomUUID();
        Story s = Story.builder()
                .id(storyId)
                .title("Test Title")
                .content("Test Content")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(s));

        var resp = svc.findStoryResponseById(storyId.toString());
        assertTrue(resp.isPresent());
        var storyResp = resp.get();

        assertNotNull(storyResp.getConditionNames());
        assertTrue(storyResp.getConditionNames().isEmpty());
    }

    @Test
    void mapStoryToResponse_nullOwner() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        UUID storyId = java.util.UUID.randomUUID();
        Story s = Story.builder()
                .id(storyId)
                .title("Test Title")
                .content("Test Content")
                .owner(null)
                .conditions(java.util.Set.of())
                .build();

        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(s));

        var resp = svc.findStoryResponseById(storyId.toString());
        assertTrue(resp.isPresent());
        var storyResp = resp.get();

        assertNull(storyResp.getOwnerUsername());
        assertNull(storyResp.getOwnerId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_storiesWithMixedConditions() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner1 = new Account();
        owner1.setUsername("owner1");
        owner1.setId(java.util.UUID.randomUUID());

        Account owner2 = new Account();
        owner2.setUsername("owner2");
        owner2.setId(java.util.UUID.randomUUID());

        Condition cond1 = new Condition();
        cond1.setName("COND1");
        Condition cond2 = new Condition();
        cond2.setName("COND2");

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story with conditions")
                .content("Content")
                .owner(owner1)
                .conditions(java.util.Set.of(cond1, cond2))
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story without conditions")
                .content("Content")
                .owner(owner2)
                .conditions(null)
                .build();

        Story s3 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story with empty conditions")
                .content("Content")
                .owner(null)
                .conditions(java.util.Set.of())
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1, s2, s3));
        when(storyRepo.searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10);

        assertNotNull(result);
        assertEquals(3, result.size());

        // Story 1: has conditions and owner
        assertEquals(2, result.get(0).getConditionNames().size());
        assertEquals("owner1", result.get(0).getOwnerUsername());
        assertEquals(owner1.getId().toString(), result.get(0).getOwnerId());

        // Story 2: null conditions, has owner
        assertTrue(result.get(1).getConditionNames().isEmpty());
        assertEquals("owner2", result.get(1).getOwnerUsername());
        assertEquals(owner2.getId().toString(), result.get(1).getOwnerId());

        // Story 3: empty conditions, null owner
        assertTrue(result.get(2).getConditionNames().isEmpty());
        assertNull(result.get(2).getOwnerUsername());
        assertNull(result.get(2).getOwnerId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_withQueryParam_filtersStories() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("owner1");

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Java Programming")
                .content("Learn Java")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1));
        when(storyRepo.searchByQueryAndConditionNames(eq("Java"), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "Java", "", "");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java Programming", result.get(0).getTitle());
        verify(storyRepo, times(1)).searchByQueryAndConditionNames(eq("Java"), isNull(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_withQueryParam_noMatches() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.empty());
        when(storyRepo.searchByQueryAndConditionNames(eq("NonExistent"), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "NonExistent", "", "");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(storyRepo, times(1)).searchByQueryAndConditionNames(eq("NonExistent"), isNull(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_withQueryParamMatchingContent_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("owner1");

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("My Story")
                .content("This content contains the word Python")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1));
        when(storyRepo.searchByQueryAndConditionNames(eq("Python"), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "Python", "", "");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("My Story", result.get(0).getTitle());
        assertTrue(result.get(0).getContent().contains("Python"));
        verify(storyRepo, times(1)).searchByQueryAndConditionNames(eq("Python"), isNull(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_withEmptyQueryParam_returnsAll() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account owner = new Account();
        owner.setUsername("owner1");

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 1")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 2")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1, s2));
        when(storyRepo.searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(storyRepo, times(1)).searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class));
    }

    // ========== Additional Tests for 100% Coverage ==========

    @Test
    void createStoryForUser_withMultipleConditions_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond1 = new Condition();
        cond1.setName("Condition1");
        Condition cond2 = new Condition();
        cond2.setName("Condition2");
        Condition cond3 = new Condition();
        cond3.setName("Condition3");
        a.setConditions(java.util.Set.of(cond1, cond2, cond3));
        when(accRepo.findWithConditionsByUsername("user1")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("Multi Condition Story", "Story with multiple conditions",
                List.of("Condition1", "Condition2", "Condition3"));

        when(storyRepo.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));

        Story created = svc.createStoryForUser("user1", req);

        assertNotNull(created);
        assertEquals("Multi Condition Story", created.getTitle());
        assertEquals("Story with multiple conditions", created.getContent());
        assertEquals(3, created.getConditions().size());
        verify(storyRepo, times(1)).save(any(Story.class));
    }

    @Test
    void createStoryForUser_withTitleAndContentWhitespace_trimmed() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("user2")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("  Title with spaces  ", "  Content with spaces  ", List.of("C1"));

        when(storyRepo.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));

        Story created = svc.createStoryForUser("user2", req);

        assertNotNull(created);
        assertEquals("Title with spaces", created.getTitle());
        assertEquals("Content with spaces", created.getContent());
        verify(storyRepo, times(1)).save(any(Story.class));
    }

    @Test
    void createStoryForUser_nullTitle_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("user3")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest(null, "Content only", List.of("C1"));

        when(storyRepo.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));

        // This tests the behavior when title is null - the trim() will throw NullPointerException
        assertThrows(NullPointerException.class, () -> svc.createStoryForUser("user3", req));
    }

    @Test
    void createStoryForUser_nullContent_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("user4")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("Title", null, List.of("C1"));

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.createStoryForUser("user4", req));
        assertEquals("Story content must not be empty", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void createStoryForUser_nullConditionNames_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("user5")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("Title", "Content", null);

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.createStoryForUser("user5", req));
        assertEquals("At least one condition must be specified", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void createStoryForUser_accountWithNullConditions_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        a.setConditions(null);
        when(accRepo.findWithConditionsByUsername("user6")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("Title", "Content", List.of("C1"));

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.createStoryForUser("user6", req));
        assertEquals("Condition(s) not linked to account: C1", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void createStoryForUser_multipleConditionsNotLinked_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond1 = new Condition();
        cond1.setName("C1");
        a.setConditions(java.util.Set.of(cond1));
        when(accRepo.findWithConditionsByUsername("user7")).thenReturn(java.util.Optional.of(a));

        StoryRequest req = new StoryRequest("Title", "Content", List.of("C1", "C2", "C3"));

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.createStoryForUser("user7", req));
        assertTrue(ex.getMessage().contains("Condition(s) not linked to account:"));
        assertTrue(ex.getMessage().contains("C2"));
        assertTrue(ex.getMessage().contains("C3"));
        verifyNoInteractions(storyRepo);
    }

    // ========== UPDATE STORY TESTS ==========

    @Test
    void updateStoryForUser_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("alice")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Updated Title", "Updated content", List.of("C1"));

        when(storyRepo.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));

        Story updated = svc.updateStoryForUser("alice", req);

        assertNotNull(updated);
        assertEquals(storyId, updated.getId());
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated content", updated.getContent());
        assertEquals(1, updated.getConditions().size());
        assertEquals(a, updated.getOwner());

        verify(storyRepo, times(1)).save(any(Story.class));
    }

    @Test
    void updateStoryForUser_withMultipleConditions_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond1 = new Condition();
        cond1.setName("Condition1");
        Condition cond2 = new Condition();
        cond2.setName("Condition2");
        a.setConditions(java.util.Set.of(cond1, cond2));
        when(accRepo.findWithConditionsByUsername("bob")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Multi Update", "Updated with multiple conditions",
                List.of("Condition1", "Condition2"));

        when(storyRepo.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));

        Story updated = svc.updateStoryForUser("bob", req);

        assertNotNull(updated);
        assertEquals(storyId, updated.getId());
        assertEquals("Multi Update", updated.getTitle());
        assertEquals(2, updated.getConditions().size());
        verify(storyRepo, times(1)).save(any(Story.class));
    }

    @Test
    void updateStoryForUser_withTitleAndContentWhitespace_trimmed() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("charlie")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "  Whitespace Title  ", "  Whitespace Content  ",
                List.of("C1"));

        when(storyRepo.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));

        Story updated = svc.updateStoryForUser("charlie", req);

        assertNotNull(updated);
        assertEquals("Whitespace Title", updated.getTitle());
        assertEquals("Whitespace Content", updated.getContent());
        verify(storyRepo, times(1)).save(any(Story.class));
    }

    @Test
    void updateStoryForUser_conditionNotLinked_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("dave")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Title", "Content", List.of("NOT_LINKED"));

        assertThrows(IllegalArgumentException.class, () -> svc.updateStoryForUser("dave", req));
        verifyNoInteractions(storyRepo);
    }

    @Test
    void updateStoryForUser_emptyContent_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("eve")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Title", "   ", List.of("C1"));

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.updateStoryForUser("eve", req));
        assertEquals("Story content must not be empty", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void updateStoryForUser_nullContent_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("frank")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Title", null, List.of("C1"));

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.updateStoryForUser("frank", req));
        assertEquals("Story content must not be empty", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void updateStoryForUser_noConditions_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        a.setConditions(java.util.Set.of());
        when(accRepo.findWithConditionsByUsername("grace")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Title", "Content", java.util.List.of());

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.updateStoryForUser("grace", req));
        assertEquals("At least one condition must be specified", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void updateStoryForUser_nullConditionNames_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("heidi")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Title", "Content", null);

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.updateStoryForUser("heidi", req));
        assertEquals("At least one condition must be specified", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void updateStoryForUser_accountNotFound_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        when(accRepo.findWithConditionsByUsername("nonexistent")).thenReturn(java.util.Optional.empty());

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Title", "Content", List.of("C1"));

        assertThrows(be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException.class,
                () -> svc.updateStoryForUser("nonexistent", req));
        verifyNoInteractions(storyRepo);
    }

    @Test
    void updateStoryForUser_accountWithNullConditions_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        a.setConditions(null);
        when(accRepo.findWithConditionsByUsername("ivan")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Title", "Content", List.of("C1"));

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.updateStoryForUser("ivan", req));
        assertEquals("Condition(s) not linked to account: C1", ex.getMessage());
        verifyNoInteractions(storyRepo);
    }

    @Test
    void updateStoryForUser_multipleConditionsNotLinked_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond1 = new Condition();
        cond1.setName("C1");
        a.setConditions(java.util.Set.of(cond1));
        when(accRepo.findWithConditionsByUsername("judy")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Title", "Content", List.of("C1", "C2", "C3"));

        var ex = assertThrows(IllegalArgumentException.class, () -> svc.updateStoryForUser("judy", req));
        assertTrue(ex.getMessage().contains("Condition(s) not linked to account:"));
        assertTrue(ex.getMessage().contains("C2"));
        assertTrue(ex.getMessage().contains("C3"));
        verifyNoInteractions(storyRepo);
    }

    @Test
    void updateStoryForUser_nullTitle_throwsNullPointerException() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("karl")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, null, "Content", List.of("C1"));

        // This tests the behavior when title is null - the trim() will throw NullPointerException
        assertThrows(NullPointerException.class, () -> svc.updateStoryForUser("karl", req));
    }

    @Test
    void updateStoryForUser_changeConditions_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond1 = new Condition();
        cond1.setName("C1");
        Condition cond2 = new Condition();
        cond2.setName("C2");
        Condition cond3 = new Condition();
        cond3.setName("C3");
        a.setConditions(java.util.Set.of(cond1, cond2, cond3));
        when(accRepo.findWithConditionsByUsername("linda")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();
        // Update to use different conditions than originally
        StoryUpdateRequest req = new StoryUpdateRequest(storyId, "Changed Conditions", "New content",
                List.of("C2", "C3"));

        when(storyRepo.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));

        Story updated = svc.updateStoryForUser("linda", req);

        assertNotNull(updated);
        assertEquals(storyId, updated.getId());
        assertEquals(2, updated.getConditions().size());
        assertTrue(updated.getConditions().stream().anyMatch(c -> c.getName().equals("C2")));
        assertTrue(updated.getConditions().stream().anyMatch(c -> c.getName().equals("C3")));
        verify(storyRepo, times(1)).save(any(Story.class));
    }

    @Test
    void updateStoryForUser_sameIdMultipleTimes_success() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryService svc = createService(storyRepo, accRepo);

        Account a = new Account();
        Condition cond = new Condition();
        cond.setName("C1");
        a.setConditions(java.util.Set.of(cond));
        when(accRepo.findWithConditionsByUsername("mike")).thenReturn(java.util.Optional.of(a));

        UUID storyId = UUID.randomUUID();

        when(storyRepo.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));

        // First update
        StoryUpdateRequest req1 = new StoryUpdateRequest(storyId, "Version 1", "Content 1", List.of("C1"));
        Story updated1 = svc.updateStoryForUser("mike", req1);
        assertEquals("Version 1", updated1.getTitle());

        // Second update with same ID
        StoryUpdateRequest req2 = new StoryUpdateRequest(storyId, "Version 2", "Content 2", List.of("C1"));
        Story updated2 = svc.updateStoryForUser("mike", req2);
        assertEquals("Version 2", updated2.getTitle());
        assertEquals(storyId, updated2.getId());

        verify(storyRepo, times(2)).save(any(Story.class));
    }

    // ===== DELETE STORY TESTS =====

    @Test
    void deleteStoryForUser_closesTicketsAndDeletesStory() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        TicketRepository ticketRepo = mock(TicketRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo, ticketRepo);

        UUID storyId = UUID.randomUUID();
        Account owner = new Account();
        owner.setId(UUID.randomUUID());
        owner.setUsername("owner");
        Story story = Story.builder().id(storyId).owner(owner).build();

        Ticket t1 = Ticket.builder().id(UUID.randomUUID()).state(TicketState.OPEN).story(story).build();
        Ticket t2 = Ticket.builder().id(UUID.randomUUID()).state(TicketState.OPEN).story(story).build();

        when(accRepo.findByUsername("owner")).thenReturn(java.util.Optional.of(owner));
        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(story));
        when(ticketRepo.findAllByStoryId(storyId)).thenReturn(java.util.List.of(t1, t2));

        svc.deleteStoryForUser("owner", storyId.toString());

        assertEquals(TicketState.CLOSED, t1.getState());
        assertEquals(TicketState.CLOSED, t2.getState());
        assertNull(t1.getStory());
        assertNull(t2.getStory());
        verify(ticketRepo, times(1)).saveAll(java.util.List.of(t1, t2));
        verify(storyRepo, times(1)).delete(story);
    }

    @Test
    void deleteStoryForUser_notOwner_throwsNotFound() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        TicketRepository ticketRepo = mock(TicketRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo, ticketRepo);

        UUID storyId = UUID.randomUUID();
        Account owner = new Account();
        owner.setId(UUID.randomUUID());
        owner.setUsername("owner");
        Story story = Story.builder().id(storyId).owner(owner).build();

        Account other = new Account();
        other.setId(UUID.randomUUID());
        other.setUsername("other");

        when(accRepo.findByUsername("other")).thenReturn(java.util.Optional.of(other));
        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(story));

        assertThrows(be.codeforbelgium.openinzichten.exceptions.StoryNotFoundException.class,
                () -> svc.deleteStoryForUser("other", storyId.toString()));
        verifyNoInteractions(ticketRepo);
        verify(storyRepo, never()).delete(any());
    }

    // ===== LIKE / UNLIKE TESTS FOR EXTRA COVERAGE =====

    @Test
    void likeStory_success_setsLikedAndCounts() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        UUID storyId = UUID.randomUUID();
        Account owner = new Account();
        owner.setId(UUID.randomUUID());
        owner.setUsername("jane");
        owner.setHasCondition(true);

        Story story = Story.builder().id(storyId).title("T").content("C").owner(owner).conditions(java.util.Set.of()).build();

        when(accRepo.findByUsername("jane")).thenReturn(java.util.Optional.of(owner));
        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(story));
        when(likeRepo.existsByStoryIdAndAccountId(storyId, owner.getId())).thenReturn(false);
        when(likeRepo.countByStoryId(storyId)).thenReturn(1L);
        when(accRepo.findByUsername(owner.getUsername())).thenReturn(java.util.Optional.of(owner));

        StoryResponse resp = svc.likeStory("jane", storyId.toString());
        assertNotNull(resp);
        assertEquals(1L, resp.getLikeCount());
        assertTrue(resp.isLikedByCurrentUser());
    }

    @Test
    void likeStory_alreadyLiked_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        UUID storyId = UUID.randomUUID();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("john");
        Story story = Story.builder().id(storyId).title("T").content("C").owner(account).build();

        when(accRepo.findByUsername("john")).thenReturn(java.util.Optional.of(account));
        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(story));
        when(likeRepo.existsByStoryIdAndAccountId(storyId, account.getId())).thenReturn(true);

        assertThrows(be.codeforbelgium.openinzichten.exceptions.StoryAlreadyLikedException.class,
                () -> svc.likeStory("john", storyId.toString()));
    }

    @Test
    void likeStory_storyNotFound_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        UUID storyId = UUID.randomUUID();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("amy");

        when(accRepo.findByUsername("amy")).thenReturn(java.util.Optional.of(account));
        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.empty());

        assertThrows(be.codeforbelgium.openinzichten.exceptions.StoryNotFoundException.class,
                () -> svc.likeStory("amy", storyId.toString()));
    }

    @Test
    void likeStory_accountNotFound_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        UUID storyId = UUID.randomUUID();
        when(accRepo.findByUsername("nouser")).thenReturn(java.util.Optional.empty());

        assertThrows(be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException.class,
                () -> svc.likeStory("nouser", storyId.toString()));
    }

    @Test
    void unlikeStory_success_decrementsAndUnsetsLiked() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        UUID storyId = UUID.randomUUID();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("paul");
        account.setHasCondition(false);
        Story story = Story.builder().id(storyId).title("T").content("C").owner(account).build();
        StoryLike like = StoryLike.builder().story(story).account(account).build();

        when(accRepo.findByUsername("paul")).thenReturn(java.util.Optional.of(account));
        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(story));
        when(likeRepo.findByStoryIdAndAccountId(storyId, account.getId())).thenReturn(java.util.Optional.of(like));
        when(likeRepo.countByStoryId(storyId)).thenReturn(0L);
        when(accRepo.findByUsername(account.getUsername())).thenReturn(java.util.Optional.of(account));

        StoryResponse resp = svc.unlikeStory("paul", storyId.toString());
        assertNotNull(resp);
        assertEquals(0L, resp.getLikeCount());
        assertFalse(resp.isLikedByCurrentUser());
    }

    @Test
    void unlikeStory_notLiked_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        UUID storyId = UUID.randomUUID();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("zoe");
        Story story = Story.builder().id(storyId).title("T").content("C").owner(account).build();

        when(accRepo.findByUsername("zoe")).thenReturn(java.util.Optional.of(account));
        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.of(story));
        when(likeRepo.findByStoryIdAndAccountId(storyId, account.getId())).thenReturn(java.util.Optional.empty());

        assertThrows(be.codeforbelgium.openinzichten.exceptions.StoryNotLikedException.class,
                () -> svc.unlikeStory("zoe", storyId.toString()));
    }

    @Test
    void unlikeStory_storyNotFound_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        UUID storyId = UUID.randomUUID();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("kate");

        when(accRepo.findByUsername("kate")).thenReturn(java.util.Optional.of(account));
        when(storyRepo.findById(storyId)).thenReturn(java.util.Optional.empty());

        assertThrows(be.codeforbelgium.openinzichten.exceptions.StoryNotFoundException.class,
                () -> svc.unlikeStory("kate", storyId.toString()));
    }

    @Test
    void unlikeStory_accountNotFound_throws() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        UUID storyId = UUID.randomUUID();
        when(accRepo.findByUsername("noacct")).thenReturn(java.util.Optional.empty());

        assertThrows(be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException.class,
                () -> svc.unlikeStory("noacct", storyId.toString()));
    }

    @Test
    void mapToResponse_withBlankCurrentUser_notLiked() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner");
        owner.setId(UUID.randomUUID());
        Story s = Story.builder().id(UUID.randomUUID()).title("T").content("C").owner(owner).build();

        StoryResponse resp = svc.mapToResponse(s, "   ");
        assertNotNull(resp);
        assertFalse(resp.isLikedByCurrentUser());
    }

    // ===== likedByCurrentUser branch tests =====

    @Test
    void mapToResponse_likedByCurrentUser_true() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account account = new Account();
        UUID accountId = UUID.randomUUID();
        account.setId(accountId);
        account.setUsername("likedUser");

        UUID storyId = UUID.randomUUID();
        Story story = Story.builder().id(storyId).title("T").content("C").owner(account).build();

        when(accRepo.findByUsername("likedUser")).thenReturn(java.util.Optional.of(account));
        when(likeRepo.existsByStoryIdAndAccountId(storyId, accountId)).thenReturn(true);
        when(likeRepo.countByStoryId(storyId)).thenReturn(5L);
        // owner lookup for livesWith
        when(accRepo.findByUsername("likedUser")).thenReturn(java.util.Optional.of(account));

        StoryResponse resp = svc.mapToResponse(story, "likedUser");
        assertNotNull(resp);
        assertTrue(resp.isLikedByCurrentUser());
        assertEquals(5L, resp.getLikeCount());
    }

    @Test
    void mapToResponse_likedByCurrentUser_false() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account account = new Account();
        UUID accountId = UUID.randomUUID();
        account.setId(accountId);
        account.setUsername("unlikedUser");

        UUID storyId = UUID.randomUUID();
        Story story = Story.builder().id(storyId).title("T").content("C").owner(account).build();

        when(accRepo.findByUsername("unlikedUser")).thenReturn(java.util.Optional.of(account));
        when(likeRepo.existsByStoryIdAndAccountId(storyId, accountId)).thenReturn(false);
        when(likeRepo.countByStoryId(storyId)).thenReturn(2L);
        when(accRepo.findByUsername("unlikedUser")).thenReturn(java.util.Optional.of(account));

        StoryResponse resp = svc.mapToResponse(story, "unlikedUser");
        assertNotNull(resp);
        assertFalse(resp.isLikedByCurrentUser());
        assertEquals(2L, resp.getLikeCount());
    }

    @Test
    void mapToResponse_likedByCurrentUser_storyIdNull_false() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("nullStoryUser");

        Story story = Story.builder().id(null).title("T").content("C").owner(account).build();

        when(accRepo.findByUsername("nullStoryUser")).thenReturn(java.util.Optional.of(account));
        // even if exists() would return true, the null story id prevents evaluation; mock defensively
        when(likeRepo.existsByStoryIdAndAccountId(any(), any())).thenReturn(true);
        when(likeRepo.countByStoryId(null)).thenReturn(0L);
        when(accRepo.findByUsername("nullStoryUser")).thenReturn(java.util.Optional.of(account));

        StoryResponse resp = svc.mapToResponse(story, "nullStoryUser");
        assertNotNull(resp);
        assertFalse(resp.isLikedByCurrentUser());
        assertEquals(0L, resp.getLikeCount());
    }

    // ===== ORDER BY TESTS =====

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByNull_noSortingApplied() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 1")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 2")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1, s2));
        when(storyRepo.searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Story 1", result.get(0).getTitle());
        assertEquals("Story 2", result.get(1).getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByEmpty_noSortingApplied() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 1")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 2")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1, s2));
        when(storyRepo.searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Story 1", result.get(0).getTitle());
        assertEquals("Story 2", result.get(1).getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByPopular_sortsByLikeCountDescending() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        UUID story1Id = java.util.UUID.randomUUID();
        Story s1 = Story.builder()
                .id(story1Id)
                .title("Story with 5 likes")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        UUID story2Id = java.util.UUID.randomUUID();
        Story s2 = Story.builder()
                .id(story2Id)
                .title("Story with 10 likes")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        UUID story3Id = java.util.UUID.randomUUID();
        Story s3 = Story.builder()
                .id(story3Id)
                .title("Story with 2 likes")
                .content("Content 3")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        // Mock like counts
        when(likeRepo.countByStoryId(story1Id)).thenReturn(5L);
        when(likeRepo.countByStoryId(story2Id)).thenReturn(10L);
        when(likeRepo.countByStoryId(story3Id)).thenReturn(2L);

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        // Repository returns stories sorted by popularity (descending)
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s2, s1, s3));
        when(storyRepo.searchByQueryAndConditionNamesOrderByPopular(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "popular");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Story with 10 likes", result.get(0).getTitle());
        assertEquals(10L, result.get(0).getLikeCount());
        assertEquals("Story with 5 likes", result.get(1).getTitle());
        assertEquals(5L, result.get(1).getLikeCount());
        assertEquals("Story with 2 likes", result.get(2).getTitle());
        assertEquals(2L, result.get(2).getLikeCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByPopularCaseInsensitive_sortsByLikeCountDescending() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        UUID story1Id = java.util.UUID.randomUUID();
        Story s1 = Story.builder()
                .id(story1Id)
                .title("Story with 3 likes")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        UUID story2Id = java.util.UUID.randomUUID();
        Story s2 = Story.builder()
                .id(story2Id)
                .title("Story with 7 likes")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        when(likeRepo.countByStoryId(story1Id)).thenReturn(3L);
        when(likeRepo.countByStoryId(story2Id)).thenReturn(7L);

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        // Repository returns stories sorted by popularity (descending)
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s2, s1));
        when(storyRepo.searchByQueryAndConditionNamesOrderByPopular(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "POPULAR");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Story with 7 likes", result.get(0).getTitle());
        assertEquals("Story with 3 likes", result.get(1).getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByRecent_sortsByCreatedAtDescending() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime lastWeek = now.minusDays(7);

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story from last week")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(lastWeek)
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story from today")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(now)
                .build();

        Story s3 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story from yesterday")
                .content("Content 3")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(yesterday)
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        // Repository returns stories sorted by creation date (descending)
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s2, s3, s1));
        when(storyRepo.searchByQueryAndConditionNamesOrderByNewest(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "recent");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Story from today", result.get(0).getTitle());
        assertEquals("Story from yesterday", result.get(1).getTitle());
        assertEquals("Story from last week", result.get(2).getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByRecentCaseInsensitive_sortsByCreatedAtDescending() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Older Story")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(yesterday)
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Newer Story")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(now)
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        // Repository returns stories sorted by creation date (descending)
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s2, s1));
        when(storyRepo.searchByQueryAndConditionNamesOrderByNewest(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "RECENT");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Newer Story", result.get(0).getTitle());
        assertEquals("Older Story", result.get(1).getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByRecent_withNullDates_nullsSortedLast() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        LocalDateTime now = LocalDateTime.now();

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story with null date")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(null)
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story with date")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(now)
                .build();

        Story s3 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Another story with null date")
                .content("Content 3")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(null)
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        // Repository returns stories sorted with non-null dates first, nulls last
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s2, s1, s3));
        when(storyRepo.searchByQueryAndConditionNamesOrderByNewest(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "recent");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Story with date", result.get(0).getTitle());
        // The two null date stories should be at the end (order between them doesn't matter)
        assertTrue(result.get(1).getTitle().contains("null date"));
        assertTrue(result.get(2).getTitle().contains("null date"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByRecent_allNullDates_returnsInOriginalOrder() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 1")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(null)
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 2")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .createdAt(null)
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1, s2));
        when(storyRepo.searchByQueryAndConditionNamesOrderByNewest(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "recent");

        assertNotNull(result);
        assertEquals(2, result.size());
        // Since both are null, comparator returns 0, so they stay in original order
        assertEquals("Story 1", result.get(0).getTitle());
        assertEquals("Story 2", result.get(1).getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByInvalidValue_noSortingApplied() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        Story s1 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 1")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        Story s2 = Story.builder()
                .id(java.util.UUID.randomUUID())
                .title("Story 2")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1, s2));
        when(storyRepo.searchByQueryAndConditionNames(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "invalid");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Story 1", result.get(0).getTitle());
        assertEquals("Story 2", result.get(1).getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByPopular_withZeroLikes_sortsCorrectly() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        UUID story1Id = java.util.UUID.randomUUID();
        Story s1 = Story.builder()
                .id(story1Id)
                .title("Story with 0 likes")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        UUID story2Id = java.util.UUID.randomUUID();
        Story s2 = Story.builder()
                .id(story2Id)
                .title("Story with 5 likes")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        UUID story3Id = java.util.UUID.randomUUID();
        Story s3 = Story.builder()
                .id(story3Id)
                .title("Story with 0 likes also")
                .content("Content 3")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        when(likeRepo.countByStoryId(story1Id)).thenReturn(0L);
        when(likeRepo.countByStoryId(story2Id)).thenReturn(5L);
        when(likeRepo.countByStoryId(story3Id)).thenReturn(0L);

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        // Repository returns stories sorted by popularity (descending)
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s2, s1, s3));
        when(storyRepo.searchByQueryAndConditionNamesOrderByPopular(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "popular");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Story with 5 likes", result.get(0).getTitle());
        assertEquals(5L, result.get(0).getLikeCount());
        // The two stories with 0 likes will be at the end (order between them doesn't matter)
        assertEquals(0L, result.get(1).getLikeCount());
        assertEquals(0L, result.get(2).getLikeCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPagedStories_orderByPopular_withSameLikeCounts_maintainsStableOrder() {
        StoryRepository storyRepo = mock(StoryRepository.class);
        AccountRepository accRepo = mock(AccountRepository.class);
        StoryLikeRepository likeRepo = mock(StoryLikeRepository.class);
        StoryService svc = createService(storyRepo, accRepo, likeRepo);

        Account owner = new Account();
        owner.setUsername("owner1");
        owner.setId(java.util.UUID.randomUUID());

        UUID story1Id = java.util.UUID.randomUUID();
        Story s1 = Story.builder()
                .id(story1Id)
                .title("Story A")
                .content("Content 1")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        UUID story2Id = java.util.UUID.randomUUID();
        Story s2 = Story.builder()
                .id(story2Id)
                .title("Story B")
                .content("Content 2")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        UUID story3Id = java.util.UUID.randomUUID();
        Story s3 = Story.builder()
                .id(story3Id)
                .title("Story C")
                .content("Content 3")
                .owner(owner)
                .conditions(java.util.Set.of())
                .build();

        // All have the same like count
        when(likeRepo.countByStoryId(story1Id)).thenReturn(3L);
        when(likeRepo.countByStoryId(story2Id)).thenReturn(3L);
        when(likeRepo.countByStoryId(story3Id)).thenReturn(3L);

        org.springframework.data.domain.Page<Story> page = mock(org.springframework.data.domain.Page.class);
        when(page.stream()).thenReturn(java.util.stream.Stream.of(s1, s2, s3));
        when(storyRepo.searchByQueryAndConditionNamesOrderByPopular(eq(""), isNull(), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = svc.getPagedStories(0, 10, null, "", "", "popular");

        assertNotNull(result);
        assertEquals(3, result.size());
        // All have the same count, should maintain original order
        assertEquals("Story A", result.get(0).getTitle());
        assertEquals("Story B", result.get(1).getTitle());
        assertEquals("Story C", result.get(2).getTitle());
        assertEquals(3L, result.get(0).getLikeCount());
        assertEquals(3L, result.get(1).getLikeCount());
        assertEquals(3L, result.get(2).getLikeCount());
    }
}


