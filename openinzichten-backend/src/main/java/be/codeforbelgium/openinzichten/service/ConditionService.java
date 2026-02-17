package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.AllConditionsMunicipalityCount;
import be.codeforbelgium.openinzichten.api.response.MunicipalityCommunitymemberCountResponse;
import be.codeforbelgium.openinzichten.api.response.SingleConditionMunicipalityCount;
import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.domain.Condition;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import be.codeforbelgium.openinzichten.repository.ConditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ConditionService {

    private static final String UNKNOWN = "Unknown";
    private final ConditionRepository conditionRepository;
    private final AccountRepository accountRepository;
    private final MunicipalityGeoJsonService municipalityGeoJsonService;

    public Optional<Condition> findConditionByName(String name) {
        return conditionRepository.findByName(name);
    }

    public List<Condition> findAll(int limit) {
        if (limit > 0) {
            return conditionRepository.findAll(PageRequest.of(0, limit)).getContent();
        }
        return conditionRepository.findAll();
    }

    public List<Condition> searchByName(String query) {
        return conditionRepository.findByNameContainingIgnoreCase(query);
    }

    /**
     * Return the set of Conditions that belong to the given account username.
     */
    public Set<Condition> getConditionsForUser(String username) {
        Account owner = accountRepository.findWithConditionsByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found: " + username));

        return owner.getConditions() == null ? Collections.emptySet() : owner.getConditions();
    }

    /**
     * Aggregate number of accounts that have the given condition, grouped by
     * municipality (gemeente).
     */
    public List<SingleConditionMunicipalityCount> getCountsByMunicipality(String conditionName) {
        List<Object[]> rows = accountRepository.countAccountsBySpecificConditionGroupedByMunicipality(conditionName);
        List<SingleConditionMunicipalityCount> res = new ArrayList<>();
        for (Object[] r : rows) {
            String code = r[0] == null ? null : r[0].toString();
            String gemeente = r[1] == null ? UNKNOWN : r[1].toString();
            Number n = (Number) r[2];
            long count = n == null ? 0L : n.longValue();
            // code may already be present from Zipcode table; fallback to mapping by name
            if (code == null || code.isBlank()) {
                code = municipalityGeoJsonService.findCodeByMunicipalityName(gemeente);
            }
            res.add(new SingleConditionMunicipalityCount(gemeente, code, count));
        }
        return res;
    }

    /**
     * Return counts grouped by municipality and broken down by condition name.
     * Each returned item contains a map of condition -> count for that
     * municipality.
     */
    public List<AllConditionsMunicipalityCount> getCountsByMunicipalityAllConditions() {
        List<Object[]> rows = accountRepository.countAccountsByAllConditionsGroupedByMunicipality();
        // Map key: municipalityCode (may be null) + gemeente name to group rows
        Map<String, AllConditionsMunicipalityCount> map = new HashMap<>();

        for (Object[] r : rows) {
            String code = r[0] == null ? null : r[0].toString();
            String gemeente = r[1] == null ? UNKNOWN : r[1].toString();
            String condition = r[2] == null ? UNKNOWN : r[2].toString();
            Number n = (Number) r[3];
            long count = n == null ? 0L : n.longValue();

            String resolvedCode = resolveMunicipalityCode(code, gemeente);

            String key = (resolvedCode == null ? "#" : resolvedCode) + "|" + gemeente;
            AllConditionsMunicipalityCount existing = map.computeIfAbsent(key, k -> {
                Map<String, Long> counts = new HashMap<>();
                counts.put(condition, count);
                return new AllConditionsMunicipalityCount(gemeente, resolvedCode, counts);
            });
            existing.getCountsByCondition().put(condition, count);
        }

        return new ArrayList<>(map.values());
    }

    private String resolveMunicipalityCode(String code, String gemeente) {
        if (code == null || code.isBlank()) {
            return municipalityGeoJsonService.findCodeByMunicipalityName(gemeente);
        }
        return code;
    }

    /**
     * Count distinct community members (accounts) that have at least one condition,
     * grouped by municipality.
     * Each account is counted only once, regardless of how many conditions they
     * have.
     * This is used when filtering on "all conditions" in the heatmap.
     */
    public List<MunicipalityCommunitymemberCountResponse> getCommunitymemberCountsByMunicipality() {
        List<Object[]> rows = accountRepository.countDistinctAccountsWithConditionsGroupedByMunicipality();
        List<MunicipalityCommunitymemberCountResponse> res = new ArrayList<>();
        for (Object[] r : rows) {
            String code = r[0] == null ? null : r[0].toString();
            String gemeente = r[1] == null ? UNKNOWN : r[1].toString();
            Number n = (Number) r[2];
            long count = n == null ? 0L : n.longValue();
            // code may already be present from Zipcode table; fallback to mapping by name
            if (code == null || code.isBlank()) {
                code = municipalityGeoJsonService.findCodeByMunicipalityName(gemeente);
            }
            res.add(new MunicipalityCommunitymemberCountResponse(gemeente, code, count));
        }
        return res;
    }
}
