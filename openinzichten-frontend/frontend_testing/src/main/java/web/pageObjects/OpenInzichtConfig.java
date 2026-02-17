package web.pageObjects;

import common.JSONUtil;

public class OpenInzichtConfig {
    private static final String JSON_CONFIG_PATH =
        "src/main/java/web/pageObjects/OpenInzichtConfig.json";
    private static final String JSON_KEY = "baseUrlPROD";

    public static String getBaseUrl() {
        // 1) System property (from Maven in CI)
        String fromSystemProp = System.getProperty("sut.base.url");
        if (fromSystemProp != null && !fromSystemProp.isBlank()) {
            return fromSystemProp;
        }

        // 2) Environment variable (for local runs)
        String fromEnv = System.getenv("SUT_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        // 3) Fallback to your existing JSON config
        return JSONUtil.getValueFromJsonFile(JSON_CONFIG_PATH, JSON_KEY);
    }
}
