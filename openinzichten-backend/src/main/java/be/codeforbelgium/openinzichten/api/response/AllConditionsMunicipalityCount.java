package be.codeforbelgium.openinzichten.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllConditionsMunicipalityCount {
    private String municipality;
    private String municipalityCode;
    private Map<String, Long> countsByCondition;
}
