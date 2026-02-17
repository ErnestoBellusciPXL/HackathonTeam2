package be.codeforbelgium.openinzichten.config;

import be.codeforbelgium.openinzichten.domain.Zipcode;
import be.codeforbelgium.openinzichten.repository.ZipcodeRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Configuration
public class ZipcodeDataLoader {

    private static final Logger log = LoggerFactory.getLogger(ZipcodeDataLoader.class);

    @Bean
    CommandLineRunner loadZipcodes(ZipcodeRepository repo,
                                   be.codeforbelgium.openinzichten.service.MunicipalityGeoJsonService municipalityGeoJsonService,
                                   @Value("${app.zipcodes.resource:data/zipcodesVlaanderen.csv}") String resourcePath) {
        return createRunner(repo, municipalityGeoJsonService, resourcePath);
    }

    public CommandLineRunner loadZipcodes(ZipcodeRepository repo, String resourcePath) {
        return createRunner(repo, null, resourcePath);
    }

    private CommandLineRunner createRunner(ZipcodeRepository repo,
                                           be.codeforbelgium.openinzichten.service.MunicipalityGeoJsonService municipalityGeoJsonService,
                                           String resourcePath) {
        return args -> {
            if (repo.count() > 0) {
                log.info("Zipcodes (postcodes table) already populated ({} rows); skipping CSV load.", repo.count());
                return;
            }

            ClassPathResource res = new ClassPathResource(resourcePath);
            if (!res.exists()) {
                log.warn("No zipcode resource found at '{}' on the classpath; skipping zipcode seeding.", resourcePath);
                return;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                String content = br.lines().collect(Collectors.joining("\n"));
                if (content.isBlank())
                    return;

                String[] allLines = content.split("\\R", -1);
                String first = allLines.length > 0 ? allLines[0] : "";
                boolean hasHeader = first.toLowerCase().contains("postcode") || first.toLowerCase().contains("gemeente")
                        || first.toLowerCase().contains("zipcode");

                CSVFormat.Builder builder = CSVFormat.DEFAULT.builder()
                        .setTrim(true)
                        .setSkipHeaderRecord(hasHeader);

                if (hasHeader) {
                    builder.setHeader("postcode", "gemeente", "provincie", "gewest");
                }

                CSVFormat format = builder.build();

                int imported = 0;
                try (java.io.Reader stringReader = new java.io.StringReader(content);
                     CSVParser parser = new CSVParser(stringReader, format)) {
                    for (CSVRecord record : parser) {
                        if (record == null)
                            continue;
                        String zipcode;
                        String gemeente;
                        String provincie = "";
                        String gewest = "";

                        if (hasHeader) {
                            zipcode = record.isMapped("postcode") ? record.get("postcode")
                                    : (record.isMapped("zipcode") ? record.get("zipcode") : record.get(0));
                            gemeente = record.isMapped("gemeente") ? record.get("gemeente")
                                    : (record.size() > 1 ? record.get(1) : "");
                            provincie = record.isMapped("provincie") ? record.get("provincie")
                                    : (record.size() > 2 ? record.get(2) : "");
                            gewest = record.isMapped("gewest") ? record.get("gewest")
                                    : (record.size() > 3 ? record.get(3) : "");
                        } else {
                            if (record.size() < 2)
                                continue;
                            zipcode = record.get(0);
                            gemeente = record.get(1);
                            if (record.size() > 2)
                                provincie = record.get(2);
                            if (record.size() > 3)
                                gewest = record.get(3);
                        }

                        if (zipcode == null || gemeente == null)
                            continue;
                        zipcode = zipcode.trim();
                        gemeente = gemeente.trim();
                        provincie = provincie == null ? "" : provincie.trim();
                        gewest = gewest == null ? "" : gewest.trim();

                        if (zipcode.isEmpty() || gemeente.isEmpty())
                            continue;

                        // Try to read mun_code from CSV if present, otherwise use GeoJSON mapping
                        String munCodeFromCsv = null;
                        if (hasHeader && record.isMapped("mun_code")) {
                            munCodeFromCsv = record.get("mun_code");
                        }

                        String munCode = munCodeFromCsv != null && !munCodeFromCsv.isBlank() ? munCodeFromCsv.trim()
                                : (municipalityGeoJsonService == null ? null
                                : municipalityGeoJsonService.findCodeByMunicipalityName(gemeente));

                        Zipcode z = new Zipcode(null, zipcode, gemeente, provincie, gewest, munCode);
                        repo.save(z);
                        imported++;
                    }
                }

                log.info("Imported {} zipcodes (postcodes) from {}", imported, resourcePath);
            } catch (Exception e) {
                log.error("Failed to load zipcode resource {}", resourcePath, e);
            }
        };
    }
}
