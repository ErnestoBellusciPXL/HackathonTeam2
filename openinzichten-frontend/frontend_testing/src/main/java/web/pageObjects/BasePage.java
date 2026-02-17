package web.pageObjects;

import org.openqa.selenium.WebDriver;

public class BasePage {
    public WebDriver driver;
    public String baseurl = OpenInzichtConfig.getBaseUrl();
    public String endpoint;

    public BasePage(WebDriver driver, String endpoint) {
        this.endpoint = endpoint;
        this.driver = driver;
    }

    protected String normalizedBaseUrl() {
        return baseurl.endsWith("/") ? baseurl : baseurl + "/";
    }

    public void navigateTo() {
        String normalizedBase = normalizedBaseUrl();

        // Ensure endpoint does NOT start with a slash
        String normalizedEndpoint = endpoint.startsWith("/")
                ? endpoint.substring(1)
                : endpoint;

        String fullUrl = normalizedBase + normalizedEndpoint;

        System.out.println("Navigating to URL: " + fullUrl);

        driver.navigate().to(fullUrl);
    }

    /**
     * Clears auth state to avoid user context leakage between tests.
     * Removes localStorage token, clears cookies and reloads to a neutral page.
     */
    public void clearAuthState() {
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("try { localStorage.removeItem('token'); } catch(e){}");
        } catch (Exception ignored) {}
        try {
            driver.manage().deleteAllCookies();
        } catch (Exception ignored) {}
        try {
            driver.get(baseurl + "/login");
        } catch (Exception ignored) {}
    }
}
