package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ErrorPage extends BasePage {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final By heading = By.xpath("//h1[contains(normalize-space(), 'Deze pagina is nergens te vinden')]");
    private final By backHomeButton = By.xpath("//button[contains(normalize-space(), 'Terug naar Home')]");

    public ErrorPage(WebDriver driver) {
        super(driver, "error");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private WebElement waitAndFindClickable(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void navigateTo(String invalidPath) {
        String normalizedBase = normalizedBaseUrl();
        String fullUrl = normalizedBase + invalidPath;
        
        System.out.println("Navigating to invalid URL: " + fullUrl);
        
        driver.navigate().to(fullUrl);
    }

    public boolean isOnErrorPage() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.urlContains("/error"));
            waitAndFind(heading);
            return true;
        } catch (TimeoutException te) {
            return false;
        }
    }

    public String getHeadingText() {
        try {
            return waitAndFind(heading).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public void clickBackHomeButton() {
        try {
            WebElement button = waitAndFindClickable(backHomeButton);
            button.click();
        } catch (Exception ignored) {
        }
    }

    public boolean isBackHomeButtonVisible() {
        try {
            waitAndFind(backHomeButton);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
