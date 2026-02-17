package be.codeforbelgium.openinzichten.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class MunicipalityGeoJsonService {

    private static final Logger log = LoggerFactory.getLogger(MunicipalityGeoJsonService.class);

    private final Map<String, String> nameToCode = new HashMap<>();

    public MunicipalityGeoJsonService(
            @Value("${app.geojson.municipalities.path:../openinzichten-frontend/src/resources/georef-belgium-submunicipality.geojson}") String geojsonPath) {
        Path p;
        try {
            p = Path.of(geojsonPath).toAbsolutePath();
        } catch (Exception e) {
            log.warn("Invalid geojson path provided for municipality mapping: {}", geojsonPath, e);
            return;
        }

        Resource res = resolveGeoJsonResource(p);
        if (res == null) {
            // resolver already logged the missing resource
            return;
        }
        try {
            loadMappingsFromResource(res, p);
        } catch (IOException e) {
            log.warn("Failed to load municipality geojson from {}", geojsonPath, e);
        }
    }

    private Resource resolveGeoJsonResource(Path p) {
        try {
            Resource res = new UrlResource(p.toUri());
            if (res.exists()) {
                return res;
            }
            // fallback: try classpath resource under data/
            ClassPathResource cp = new ClassPathResource("data/georef-belgium-submunicipality.geojson");
            if (cp.exists()) {
                return cp;
            } else {
                log.warn(
                        "Municipality GeoJSON not found at {} and classpath data/georef-belgium-submunicipality.geojson missing - municipality code mapping will be empty",
                        p);
                return null;
            }
        } catch (MalformedURLException e) {
            log.warn("Invalid geojson path provided for municipality mapping: {}", p, e);
            return null;
        }
    }

    private void loadMappingsFromResource(Resource res, Path p) throws IOException {
        ObjectMapper om = new ObjectMapper();
        try (InputStream is = res.getInputStream()) {
            JsonNode root = om.readTree(is);
            JsonNode features = root.get("features");
            if (features == null || !features.isArray()) {
                return;
            }
            for (Iterator<JsonNode> it = features.elements(); it.hasNext(); ) {
                JsonNode f = it.next();
                JsonNode props = f.get("properties");
                if (props == null)
                    continue;

                String name = getTextOrFirstArray(props.get("mun_name_nl"));
                String code = getTextOrFirstArray(props.get("mun_code"));

                if (name != null && code != null) {
                    nameToCode.put(normalize(name), code);
                }
            }
        }
        log.info("Loaded {} municipality name->code mappings from {}", nameToCode.size(), p);
    }

    private String getTextOrFirstArray(JsonNode node) {
        if (node == null)
            return null;
        if (node.isArray() && node.size() > 0)
            return node.get(0).asText();
        return node.asText();
    }

    private String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }

    public String findCodeByMunicipalityName(String gemeente) {
        if (gemeente == null)
            return null;
        return nameToCode.get(normalize(gemeente));
    }
}
