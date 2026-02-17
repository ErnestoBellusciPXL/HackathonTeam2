package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.request.StoryRequest;
import be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest;
import be.codeforbelgium.openinzichten.api.response.StoryResponse;
import be.codeforbelgium.openinzichten.domain.*;
import be.codeforbelgium.openinzichten.exceptions.AccountNotFoundException;
import be.codeforbelgium.openinzichten.exceptions.StoryAlreadyLikedException;
import be.codeforbelgium.openinzichten.exceptions.StoryNotFoundException;
import be.codeforbelgium.openinzichten.exceptions.StoryNotLikedException;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.StoryLikeRepository;
import be.codeforbelgium.openinzichten.repository.StoryRepository;
import be.codeforbelgium.openinzichten.repository.TicketRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final AccountRepository accountRepository;
    private final StoryLikeRepository storyLikeRepository;
    private final TicketRepository ticketRepository;

    /**
     * Maps a Story entity to a StoryResponse DTO.
     *
     * @param story the Story entity
     * @return StoryResponse DTO
     */
    public StoryResponse mapToResponse(Story story) {
        return mapToResponse(story, (String) null);
    }

    public StoryResponse mapToResponse(Story story, String currentUsername) {
        UUID accountId = resolveAccountId(currentUsername);
        return mapStoryToResponse(story, accountId);
    }

    /**
     * Create a new story and link it to the given account (by username) and
     * specified condition names.
     *
     * @param username the account username (from security context)
     * @param request  the story request containing content and condition names
     * @return saved Story
     */
    public Story createStoryForUser(String username, StoryRequest request) {
        return saveStory(username, null, request.getTitle(), request.getContent(), request.getConditionNames());
    }

    public Optional<Story> getStoryById(UUID id) {
        return storyRepository.findById(id);
    }

    /**
     * Find all stories for a given owner id (UUID string). Throws
     * IllegalArgumentException when id is not a UUID.
     *
     * @param id owner id as string
     * @return list of mapped StoryResponse
     */
    public List<StoryResponse> findStoriesByUserId(String id) {
        return findStoriesByUserId(id, null);
    }

    public List<StoryResponse> findStoriesByUserId(String id, String currentUsername) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user id");
        }

        List<Story> stories = storyRepository.findByOwnerId(uuid);
        UUID accountId = resolveAccountId(currentUsername);

        return stories.stream()
                .map(story -> mapStoryToResponse(story, accountId))
                .toList();
    }

    /**
     * Parse the provided id and return a mapped StoryResponse if found.
     * Throws IllegalArgumentException when the id is not a valid UUID string.
     *
     * @param id the story id as string
     * @return optional StoryResponse
     */
    public Optional<StoryResponse> findStoryResponseById(String id) {
        return findStoryResponseById(id, null);
    }

    public Optional<StoryResponse> findStoryResponseById(String id, String currentUsername) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid story id");
        }

        UUID accountId = resolveAccountId(currentUsername);

        return storyRepository.findById(uuid)
                .map(story -> mapStoryToResponse(story, accountId));
    }

    public StoryResponse likeStory(String username, String storyId) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        UUID storyUuid = parseStoryId(storyId);
        Story story = storyRepository.findById(storyUuid)
                .orElseThrow(() -> new StoryNotFoundException(storyId));

        if (storyLikeRepository.existsByStoryIdAndAccountId(storyUuid, account.getId())) {
            throw new StoryAlreadyLikedException(storyId);
        }

        StoryLike like = StoryLike.builder()
                .story(story)
                .account(account)
                .build();
        storyLikeRepository.save(like);

        long likeCount = storyLikeRepository.countByStoryId(storyUuid);
        return buildStoryResponse(story, likeCount, true);
    }

    public StoryResponse unlikeStory(String username, String storyId) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        UUID storyUuid = parseStoryId(storyId);
        Story story = storyRepository.findById(storyUuid)
                .orElseThrow(() -> new StoryNotFoundException(storyId));

        StoryLike like = storyLikeRepository.findByStoryIdAndAccountId(storyUuid, account.getId())
                .orElseThrow(() -> new StoryNotLikedException(storyId));

        storyLikeRepository.delete(like);
        long likeCount = storyLikeRepository.countByStoryId(storyUuid);
        return buildStoryResponse(story, likeCount, false);
    }

    /**
     * Retrieves a paginated list of stories.
     *
     * @param page the page number (0-based)
     * @param size the number of items per page
     * @return list of mapped StoryResponse
     */
    public List<StoryResponse> getPagedStories(int page, int size) {
        return getPagedStories(page, size, null, "", "", "");
    }

    public List<StoryResponse> getPagedStories(int page, int size, String currentUsername, String query, String condString, String orderBy) {
        Pageable pageable = PageRequest.of(page, size);

        if (query == null) {
            query = "";
        }

        List<String> conditionNames = null;
        if (condString != null && !condString.isBlank()) {
            conditionNames = Arrays.stream(condString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
            if (conditionNames.isEmpty()) {
                conditionNames = null;
            }
        }

        Page<Story> storyPage;

        if (orderBy != null && !orderBy.isBlank()) {
            if ("popular".equalsIgnoreCase(orderBy)) {
                storyPage = storyRepository.searchByQueryAndConditionNamesOrderByPopular(query, conditionNames, pageable);
            } else if ("recent".equalsIgnoreCase(orderBy)) {
                storyPage = storyRepository.searchByQueryAndConditionNamesOrderByNewest(query, conditionNames, pageable);
            } else {
                storyPage = storyRepository.searchByQueryAndConditionNames(query, conditionNames, pageable);
            }
        } else {
            storyPage = storyRepository.searchByQueryAndConditionNames(query, conditionNames, pageable);
        }

        UUID accountId = resolveAccountId(currentUsername);

        return storyPage
                .stream()
                .map(story -> mapStoryToResponse(story, accountId))
                .toList();
    }

    /**
     * Retrieve a small list of recommended stories. When the user is
     * authenticated, their linked conditions are used to prioritize stories that
     * share those conditions. We interleave per-condition picks so users with
     * multiple conditions see a mix. If no user is provided or not enough
     * matches are found, the most-liked stories are returned to fill the list.
     *
     * @param currentUsername the username of the requesting user (nullable)
     * @param limit           desired number of stories (3-10)
     * @return list of mapped StoryResponse (fixed size within bounds)
     */
    public List<StoryResponse> getRecommendedStories(String currentUsername, int limit) {
        validateRecommendedLimit(limit);
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);

        Account account = findAccountWithConditions(currentUsername);
        List<Story> stories = fetchConditionBasedStories(account, limit, since);
        fillWithTopStories(stories, limit, since);
        fillWithAllTimeTopStories(stories, limit);

        final UUID viewerAccountId = account != null ? account.getId() : null;
        return stories.stream()
                .map(story -> mapStoryToResponse(story, viewerAccountId))
                .toList();
    }

    private void validateRecommendedLimit(int limit) {
        if (limit < 3 || limit > 10) {
            throw new IllegalArgumentException("Limit must be between 3 and 10");
        }
    }

    private Account findAccountWithConditions(String currentUsername) {
        if (currentUsername == null) {
            return null;
        }
        return accountRepository.findWithConditionsByUsername(currentUsername)
                .orElseThrow(() -> new AccountNotFoundException(currentUsername));
    }

    private List<Story> fetchConditionBasedStories(Account account, int limit, Instant since) {
        if (account == null) {
            return new ArrayList<>();
        }

        List<String> conditionNames = Optional.ofNullable(account.getConditions())
                .orElse(Collections.emptySet())
                .stream()
                .map(Condition::getName)
                .sorted()
                .toList();

        if (conditionNames.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<Story>> storiesByCondition = new LinkedHashMap<>();
        for (String conditionName : conditionNames) {
            List<Story> topForCondition = storyRepository
                    .findTopByConditionNameSince(conditionName, since, PageRequest.of(0, limit))
                    .getContent();
            storiesByCondition.put(conditionName, topForCondition);
        }

        return interleaveStories(storiesByCondition, limit);
    }

    private void fillWithTopStories(List<Story> stories, int limit, Instant since) {
        if (stories.size() >= limit) {
            return;
        }
        List<Story> topStories = storyRepository.findTopStoriesSince(since, PageRequest.of(0, limit)).getContent();
        appendUnique(stories, topStories, limit);
    }

    private void fillWithAllTimeTopStories(List<Story> stories, int limit) {
        if (stories.size() >= limit) {
            return;
        }
        List<Story> topStoriesAllTime = storyRepository.findTopStories(PageRequest.of(0, limit)).getContent();
        appendUnique(stories, topStoriesAllTime, limit);
    }

    private void appendUnique(List<Story> target, List<Story> candidates, int limit) {
        Set<UUID> seen = target.stream()
                .map(Story::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Story story : candidates) {
            if (target.size() >= limit) {
                break;
            }
            UUID id = story.getId();
            if (id == null || seen.add(id)) {
                target.add(story);
            }
        }
    }

    /**
     * Interleave stories per condition in round-robin order to produce a mixed
     * list. Deduplicates by story id.
     */
    private List<Story> interleaveStories(Map<String, List<Story>> storiesByCondition, int limit) {
        List<Story> result = new ArrayList<>();
        Map<String, Integer> indices = new HashMap<>();
        Set<UUID> seen = new HashSet<>();

        boolean added;
        do {
            added = false;
            for (Map.Entry<String, List<Story>> entry : storiesByCondition.entrySet()) {
                if (result.size() >= limit) {
                    break;
                }
                int idx = indices.getOrDefault(entry.getKey(), 0);
                List<Story> stories = entry.getValue();
                if (idx < stories.size()) {
                    Story candidate = stories.get(idx);
                    indices.put(entry.getKey(), idx + 1);
                    UUID id = candidate.getId();
                    if (id == null || seen.add(id)) {
                        result.add(candidate);
                        added = true;
                    }
                }
            }
        } while (added && result.size() < limit);

        return result;
    }

    @Transactional
    public void deleteStoryForUser(String username, String storyId) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        UUID storyUuid = parseStoryId(storyId);
        Story story = storyRepository.findById(storyUuid)
                .orElseThrow(() -> new StoryNotFoundException(storyId));

        if (story.getOwner() == null || story.getOwner().getId() == null
                || !story.getOwner().getId().equals(account.getId())) {
            throw new StoryNotFoundException(storyId);
        }

        closeAndDetachTickets(story);
        storyRepository.delete(story);
    }

    private StoryResponse mapStoryToResponse(Story story, UUID accountId) {
        long likeCount = resolveLikeCount(story.getId());
        boolean likedByCurrentUser = accountId != null
                && story.getId() != null
                && storyLikeRepository.existsByStoryIdAndAccountId(story.getId(), accountId);
        return buildStoryResponse(story, likeCount, likedByCurrentUser);
    }

    private StoryResponse buildStoryResponse(Story story, long likeCount, boolean likedByCurrentUser) {
        List<String> conds = story.getConditions() == null ? Collections.emptyList()
                : story.getConditions().stream().map(Condition::getName).toList();
        String ownerUsername = story.getOwner() != null ? story.getOwner().getUsername() : null;
        String ownerId = story.getOwner() != null && story.getOwner().getId() != null
                ? story.getOwner().getId().toString()
                : null;

        boolean livesWith = false;
        if (ownerUsername != null && !ownerUsername.isBlank()) {
            livesWith = accountRepository.findByUsername(ownerUsername)
                    .map(Account::isHasCondition)
                    .orElse(false);
        }
        return new StoryResponse(
                story.getId() != null ? story.getId().toString() : null,
                story.getTitle(),
                story.getContent(),
                conds,
                ownerUsername,
                ownerId,
                likeCount,
                likedByCurrentUser,
                livesWith);
    }

    private void closeAndDetachTickets(Story story) {
        if (story == null || story.getId() == null) {
            return;
        }

        List<Ticket> tickets = ticketRepository.findAllByStoryId(story.getId());
        if (tickets.isEmpty()) {
            return;
        }

        tickets.forEach(ticket -> {
            ticket.setState(TicketState.CLOSED);
            ticket.setStory(null);
        });

        ticketRepository.saveAll(tickets);
    }

    private UUID parseStoryId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid story id");
        }
    }

    private long resolveLikeCount(UUID storyId) {
        if (storyId == null) {
            return 0L;
        }
        return storyLikeRepository.countByStoryId(storyId);
    }

    private UUID resolveAccountId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return accountRepository.findByUsername(username)
                .map(Account::getId)
                .orElseThrow(() -> new AccountNotFoundException(username));
    }

    /**
     * Update an existing story for the given user.
     *
     * @param name    the account username (from security context)
     * @param request the story update request containing id, content, and condition
     *                names
     * @return updated Story
     */
    public Story updateStoryForUser(String name, @Valid StoryUpdateRequest request) {
        return saveStory(name, request.getId(), request.getTitle(), request.getContent(), request.getConditionNames());
    }

    /**
     * Shared logic for creating or updating a story.
     * Validates content, conditions, and links them to the owner's account.
     *
     * @param username       the account username (from security context)
     * @param storyId        the story ID (null for create, non-null for update)
     * @param title          the story title
     * @param content        the story content
     * @param conditionNames the list of condition names
     * @return saved Story
     */
    private Story saveStory(String username, UUID storyId, String title, String content, List<String> conditionNames) {
        Account owner = accountRepository.findWithConditionsByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(username));

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Story content must not be empty");
        }

        List<String> requestedConditionNames = Optional.ofNullable(conditionNames)
                .orElse(Collections.emptyList());

        if (requestedConditionNames.isEmpty()) {
            throw new IllegalArgumentException("At least one condition must be specified");
        }

        // Use the account's own conditions as the allowed set
        Set<Condition> ownerConditions = owner.getConditions() == null ? Collections.emptySet() : owner.getConditions();
        Set<String> ownerConditionNames = ownerConditions.stream().map(Condition::getName).collect(Collectors.toSet());

        // ensure requested conditions are linked to the account
        List<String> notLinked = requestedConditionNames.stream()
                .filter(n -> !ownerConditionNames.contains(n))
                .toList();

        if (!notLinked.isEmpty()) {
            throw new IllegalArgumentException("Condition(s) not linked to account: " + String.join(", ", notLinked));
        }

        // Build the set of Condition entities for the requested names using owner's
        // collection
        Map<String, Condition> ownerConditionMap = ownerConditions.stream()
                .collect(Collectors.toMap(Condition::getName, c -> c));
        Set<Condition> conditions = requestedConditionNames.stream().map(ownerConditionMap::get)
                .collect(Collectors.toSet());

        Story story = Story.builder()
                .id(storyId)
                .title(title.trim())
                .content(content.trim())
                .conditions(conditions)
                .owner(owner)
                .build();

        return storyRepository.save(story);
    }
}
