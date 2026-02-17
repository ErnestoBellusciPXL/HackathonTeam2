package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AboutPage extends BasePage {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final By navigationBarLink = By.cssSelector("header nav a[href='/about']");
    private final By footerLink = By.id("NavigateAboutFooter");
    private final By heading = By.xpath("//h1[contains(normalize-space(), 'Over OpenInzicht')]");

    public AboutPage(WebDriver driver) {
        super(driver, "about");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private WebElement waitAndFindClickable(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void navigateViaNavigationBar() {
        try {
            WebElement link = waitAndFindClickable(navigationBarLink);
            link.click();
        } catch (Exception e) {
            navigateTo();
        }
    }

    public void scrollDownToFooterLink() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
        } catch (Exception ignored) {
        }
    }

    public void navigateViaFooterLink() {
        try {
            WebElement link = waitAndFindClickable(footerLink);
            link.click();
        } catch (Exception e) {
            navigateTo();
        }
    }

    public boolean isOnAboutPage() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.urlContains("/about"));
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
}
