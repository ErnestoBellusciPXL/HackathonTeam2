package be.codeforbelgium.openinzichten.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SingleConditionMunicipalityCount {
    private String municipality; // human readable name
    private String municipalityCode; // stable code from GeoJSON (e.g. NIS / mun_code)
    private long count;
}
