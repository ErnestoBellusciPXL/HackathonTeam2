package web.browser;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class BrowserUtil {
    private static final String SELENIUM_HUB_URL = "http://localhost:4444/wd/hub";

    // Helper method to get the ChromeOptions with disabled password popups
    private static ChromeOptions getChromeOptions() {
        Map<String, Object> prefs = new HashMap<>();
        // Disable "Save password?" prompt
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        // Disable "Unsafe password" leak detection warning
        prefs.put("profile.password_manager_leak_detection", false);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);

        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--disable-features=PasswordManagerEnabled");
        options.addArguments("window-size=1920,1080");

        if (System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null) {
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-extensions");
            options.addArguments("--remote-allow-origins=*");
        }

        return options;
    }
    
    public static WebDriver createBrowser(String browsername) {
        WebDriver driver;
        
        // Check if running in CI/Docker environment (Selenium Grid)
        String useRemote = System.getenv("USE_REMOTE_DRIVER");
        boolean isRemote = "true".equalsIgnoreCase(useRemote);
        
        if (isRemote) {
            try {
                driver = createRemoteDriver(browsername);
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException("Failed to create remote WebDriver", e);
            }
        } else {
            driver = createLocalDriver(browsername);
        }

        driver.manage().window().maximize();
        return driver;
    }
    
    private static WebDriver createRemoteDriver(String browsername) throws MalformedURLException, URISyntaxException {
        WebDriver driver;
        switch (browsername.toLowerCase()) {
            case "chrome":
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--no-sandbox");
                chromeOptions.addArguments("--disable-dev-shm-usage");
                driver = new RemoteWebDriver(new URI(SELENIUM_HUB_URL).toURL(), chromeOptions);
                break;
            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                driver = new RemoteWebDriver(new URI(SELENIUM_HUB_URL).toURL(), firefoxOptions);
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browsername);
        }
        return driver;
    }
    
    private static WebDriver createLocalDriver(String browsername) {
        WebDriver driver;
        switch (browsername.toLowerCase()) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver(getChromeOptions());
                break;
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                driver = new org.openqa.selenium.firefox.FirefoxDriver();
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browsername);
        }
        return driver;
    }

    public static WebDriver createBrowser() {
        return createBrowser(BrowserConfig.getBrowserName());
    }
}