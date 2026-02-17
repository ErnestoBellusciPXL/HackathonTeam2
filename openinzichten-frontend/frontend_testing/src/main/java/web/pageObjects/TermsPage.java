package web.pageObjects;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class TermsPage extends BasePage{
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final By termsLink = By.id("NavigateTerms");
    private final By termsHeader = By.id("GebruikersvoorwaardenTitel");
    private final By termsLastUpdated = By.id("LaatstBijgewerktGebruikersvoorwaarden");


    public TermsPage(WebDriver driver) {
        super(driver, "terms");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void scrollDownToTermsLink() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,document.body.scrollHeight)");
    }

    public void navigateToTermsPage() {
        WebElement termsElem = waitAndFind(termsLink);
        termsElem.click();
    }

    public String getTermsHeaderText() {
        WebElement headerElem = waitAndFind(termsHeader);
        return headerElem.getText().trim();
    }

    public String getTermsLastUpdatedText() {
        WebElement lastUpdatedElem = waitAndFind(termsLastUpdated);
        return lastUpdatedElem.getText().trim();
    }

    public boolean isOnTermsPage() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.urlContains("terms"));
            return true;
        } catch (TimeoutException te) {
            return false;
        }
    }

    public boolean isLastUpdatedPresent() {
        try {
            WebElement lastUpdatedElem = waitAndFind(termsLastUpdated);
            return lastUpdatedElem.isDisplayed();
        } catch (TimeoutException te) {
            return false;
        }
    }
}
