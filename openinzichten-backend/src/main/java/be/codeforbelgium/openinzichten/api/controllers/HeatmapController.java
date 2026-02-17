package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.service.ConditionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/heatmap")
public class HeatmapController {

    private final ConditionService conditionService;

    public HeatmapController(ConditionService conditionService) {
        this.conditionService = conditionService;
    }

    @GetMapping("/counts-by-municipality")
    public ResponseEntity<Object> getCountsByMunicipality(
            @RequestParam(name = "condition", required = false) String condition) {
        if (condition == null || condition.isBlank()) {
            // Return count of unique community members per municipality (each account
            // counted once)
            var counts = conditionService.getCommunitymemberCountsByMunicipality();
            return ResponseEntity.ok(counts);
        } else {
            var counts = conditionService.getCountsByMunicipality(condition.trim());
            return ResponseEntity.ok(counts);
        }
    }
}
