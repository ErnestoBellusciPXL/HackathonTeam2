package web.browser;

import common.JSONUtil;

public class BrowserConfig {
    public static String getBrowserName() {
        return JSONUtil.getValueFromJsonFile("src/main/java/web/browser/webConfig.json", "defaultBrowser");
    }
}