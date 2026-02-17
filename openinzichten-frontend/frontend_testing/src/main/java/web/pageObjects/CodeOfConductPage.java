package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class CodeOfConductPage extends BasePage {
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final By codeOfConductLink = By.id("NavigateCodeOfConduct");
    private final By codeOfConductHeader = By.id("GedragscodeTitel");
    private final By codeOfConductLastUpdated = By.id("LaatstBijgewerktGedragscode");

    public CodeOfConductPage(WebDriver driver) {
        super(driver, "code-of-conduct");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void scrollDownToCodeOfConductLink() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,document.body.scrollHeight)");
    }

    public void navigateToCodeOfConductPage() {
        WebElement codeOfConductElem = waitAndFind(codeOfConductLink);
        codeOfConductElem.click();
    }

    public String getCodeOfConductHeaderText() {
        WebElement headerElem = waitAndFind(codeOfConductHeader);
        return headerElem.getText().trim();
    }

    public String getCodeOfConductLastUpdatedText() {
        WebElement lastUpdatedElem = waitAndFind(codeOfConductLastUpdated);
        return lastUpdatedElem.getText().trim();
    }

    public boolean isOnCodeOfConductPage() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.urlContains("code-of-conduct"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLastUpdatedPresent() {
        try {
            WebElement lastUpdatedElem = waitAndFind(codeOfConductLastUpdated);
            return lastUpdatedElem.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

}
