package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PrivacyPolicyPage extends BasePage {
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private By privacyLink = By.id("NavigatePrivacy");
    private By privacyHeader = By.id("PrivacyVerklaringtitel");
    private By privacyLastUpdated = By.id("LaatstBijgewerktPrivacyverklaring");

    public PrivacyPolicyPage(WebDriver driver) {
        super(driver, "privacy");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void scrollDownToPrivacyLink() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,document.body.scrollHeight)");
    }

    public void navigateToPrivacyPage() {
        WebElement privacyElem = waitAndFind(privacyLink);
        privacyElem.click();
    }

    public String getPrivacyHeaderText() {
        WebElement headerElem = waitAndFind(privacyHeader);
        return headerElem.getText().trim();
    }

    public String getPrivacyLastUpdatedText() {
        WebElement lastUpdatedElem = waitAndFind(privacyLastUpdated);
        return lastUpdatedElem.getText().trim();
    }

    public boolean isOnPrivacyPage() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.urlContains("privacy"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLastUpdatedPresent() {
        try {
            WebElement lastUpdatedElem = waitAndFind(privacyLastUpdated);
            return lastUpdatedElem.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
