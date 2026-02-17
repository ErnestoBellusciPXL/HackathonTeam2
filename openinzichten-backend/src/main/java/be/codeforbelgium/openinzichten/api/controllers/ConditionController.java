package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.ConditionResponse;
import be.codeforbelgium.openinzichten.service.ConditionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conditions")
public class ConditionController {

    private final ConditionService conditionService;

    public ConditionController(ConditionService conditionService) {
        this.conditionService = conditionService;
    }

    // Optional limit parameter to limit number of returned conditions
    @GetMapping
    public ResponseEntity<List<ConditionResponse>> findAll(
            @RequestParam(name = "limit", required = false) Integer limit) {
        int effectiveLimit = limit == null ? 0 : Math.max(0, limit);
        List<ConditionResponse> items = conditionService.findAll(effectiveLimit)
                .stream()
                .map(c -> new ConditionResponse(c.getName()))
                .toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/user-conditions")
    public ResponseEntity<List<ConditionResponse>> getConditionsByUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        var conditions = conditionService.getConditionsForUser(auth.getName());
        List<ConditionResponse> resp = conditions.stream()
                .map(c -> new ConditionResponse(c.getName()))
                .toList();
        return ResponseEntity.ok(resp);
    }
}
