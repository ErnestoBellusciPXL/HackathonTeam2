package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.annotation.RateLimit;
import be.codeforbelgium.openinzichten.api.request.StoryReformatRequest;
import be.codeforbelgium.openinzichten.api.request.StoryRequest;
import be.codeforbelgium.openinzichten.api.request.StoryUpdateRequest;
import be.codeforbelgium.openinzichten.api.response.StoryReformatResponse;
import be.codeforbelgium.openinzichten.api.response.StoryResponse;
import be.codeforbelgium.openinzichten.domain.Story;
import be.codeforbelgium.openinzichten.security.JwtService;
import be.codeforbelgium.openinzichten.service.AIService;
import be.codeforbelgium.openinzichten.service.StoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stories")
@Validated
public class StoryController {

    private final StoryService storyService;
    private final AIService aiService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<StoryResponse> addStoryForCurrentUser(@Valid @RequestBody StoryRequest request, HttpServletRequest req) {
        var username = getAuthenticatedUsername().orElse(null);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }

        var userId = getUserId(req);

        if (userId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var alreadyExists = storyService.findStoriesByUserId(userId.get().toString());

        if (!alreadyExists.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Story created = storyService.createStoryForUser(username, request);
        StoryResponse resp = storyService.mapToResponse(created, username);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/update-my-story")
    public ResponseEntity<StoryResponse> updateStoryForCurrentUser(@Valid @RequestBody StoryUpdateRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        var updatedStoryOpt = storyService.updateStoryForUser(auth.getName(), request);

        StoryResponse resp = storyService.mapToResponse(updatedStoryOpt);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryResponse> getStoryById(@PathVariable("id") String id) {
        var story = storyService.findStoryResponseById(id, getAuthenticatedUsername().orElse(null));
        return story.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(404).build());
    }

    @GetMapping("/byuserid/{id}")
    public ResponseEntity<List<StoryResponse>> getStoriesByUserId(@PathVariable("id") String id) {
        List<StoryResponse> stories = storyService.findStoriesByUserId(id, getAuthenticatedUsername().orElse(null));
        return ResponseEntity.ok(stories);
    }

    @GetMapping
    public ResponseEntity<Object> getPagedStories(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "condition", required = false) String condition,
            @RequestParam(name = "orderBy", required = false) String orderBy
    ) {
        if (page < 0 || size <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid_pagination", "message", "Page must be >= 0 and size must be > 0"));
        }
        var pagedStories = storyService.getPagedStories(page, size, getAuthenticatedUsername().orElse(null), query, condition, orderBy);
        return ResponseEntity.ok(pagedStories);
    }

    @GetMapping("/recommended")
    public ResponseEntity<Object> getRecommendedStories(
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        if (size < 3 || size > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid_limit", "message", "Size must be between 3 and 10"));
        }
        var stories = storyService.getRecommendedStories(getAuthenticatedUsername().orElse(null), size);
        return ResponseEntity.ok(stories);
    }

    @RateLimit(requests = 3, perSeconds = 3600)
    @PostMapping("/ai/reformat")
    public ResponseEntity<StoryReformatResponse> reformatStory(@Valid @RequestBody StoryReformatRequest storyReformatRequest) {
        String reformattedStory = aiService.doStoryReformatting(storyReformatRequest.getStoryContent());

        StoryReformatResponse response = new StoryReformatResponse();

        response.setReformattedStoryContent(reformattedStory);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<StoryResponse> likeStory(@PathVariable("id") String id) {
        var username = getAuthenticatedUsername().orElse(null);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        StoryResponse response = storyService.likeStory(username, id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/likes")
    public ResponseEntity<StoryResponse> unlikeStory(@PathVariable("id") String id) {
        var username = getAuthenticatedUsername().orElse(null);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        StoryResponse response = storyService.unlikeStory(username, id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(@PathVariable("id") String id) {
        var username = getAuthenticatedUsername().orElse(null);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        storyService.deleteStoryForUser(username, id);
        return ResponseEntity.noContent().build();
    }

    private Optional<String> getAuthenticatedUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    private Optional<UUID> getUserId(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        final String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            return Optional.empty();
        }

        return Optional.ofNullable(jwtService.extractId(token));
    }
}
