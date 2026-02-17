package be.codeforbelgium.openinzichten.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MunicipalityGeoJsonServiceTest {

    @Test
    void loadsArrayAndStringProperties_and_normalizesNames() throws Exception {
        String json = "{" +
                "\"type\":\"FeatureCollection\"," +
                "\"features\":[" +
                "{\"type\":\"Feature\",\"properties\":{\"mun_name_nl\":[\"Riemst\"],\"mun_code\":[\"12345\"]}}," +
                "{\"type\":\"Feature\",\"properties\":{\"mun_name_nl\":\"OtherTown\",\"mun_code\":\"54321\"}}" +
                "]}";

        Path tmp = Files.createTempFile("mun-geojson", ".json");
        try {
            Files.writeString(tmp, json, StandardCharsets.UTF_8);

            MunicipalityGeoJsonService svc = new MunicipalityGeoJsonService(tmp.toString());

            // exact
            assertEquals("12345", svc.findCodeByMunicipalityName("Riemst"));
            // different case / trimmed
            assertEquals("12345", svc.findCodeByMunicipalityName(" riemst "));
            // string-valued fields
            assertEquals("54321", svc.findCodeByMunicipalityName("OtherTown"));
            // missing -> null
            assertNull(svc.findCodeByMunicipalityName("Unknown"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void findCodeByMunicipalityName_returnsNullWhenInputNull() throws Exception {
        // create small valid geojson so service constructs without error
        String json = "{" +
                "\"type\":\"FeatureCollection\",\"features\":[]}";
        Path tmp = Files.createTempFile("mun-empty", ".json");
        try {
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            MunicipalityGeoJsonService svc = new MunicipalityGeoJsonService(tmp.toString());

            assertNull(svc.findCodeByMunicipalityName(null));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void loadsFromClasspath_when_filesystemPath_missing() {
        // pass a path that does not exist so constructor should fall back to classpath resource
        MunicipalityGeoJsonService svc = new MunicipalityGeoJsonService("/this/path/does/not/exist.geojson");

        assertEquals("CL123", svc.findCodeByMunicipalityName("ClasspathTown"));
    }

    @Test
    void returnsEmptyMapping_when_filesystem_and_classpath_missing() throws Exception {
        var loader = Thread.currentThread().getContextClassLoader();
        var resUrl = loader.getResource("data/georef-belgium-submunicipality.geojson");

        // If resource is not file-backed (e.g. inside a jar), skip this test
        org.junit.jupiter.api.Assumptions.assumeTrue(resUrl != null && "file".equals(resUrl.getProtocol()));

        java.nio.file.Path path = java.nio.file.Paths.get(resUrl.toURI());
        java.nio.file.Path backup = path.resolveSibling(path.getFileName().toString() + ".bak");

        // move the file out of the way so classpath lookup fails
        java.nio.file.Files.move(path, backup);
        try {
            MunicipalityGeoJsonService svc = new MunicipalityGeoJsonService("/this/path/does/not/exist.geojson");
            // since both filesystem path and classpath resource are missing, mapping should be empty
            assertNull(svc.findCodeByMunicipalityName("ClasspathTown"));
        } finally {
            // restore
            java.nio.file.Files.move(backup, path);
        }
    }
}
