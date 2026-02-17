package web.pageObjects;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class MyStoryPage extends BasePage{
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final By navigateToMyStoryButton = By.id("NavigateToMyStoryButton");
    private final By hoverOverUsername = By.id("HoverUsername");
    private final By editStoryButton = By.id("BewerkVerhaal");
    private final By deleteStoryButton = By.id("VerwijderVerhaal");
    private final By confirmDeleteButton = By.id("BevestigVerwijderenVerhaal");

    public MyStoryPage(WebDriver driver) {
        super(driver, "mijn-verhaal");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void hoverOverUsername() {
        WebElement hoverElem = waitAndFind(hoverOverUsername);
        Actions action = new Actions(driver);
        action.moveToElement(hoverElem).perform();
    }

    public void navigateToMyStory() {
        WebElement navigateBtn = waitAndFind(navigateToMyStoryButton);
        navigateBtn.click();
    }

    public void clickEditStoryButton() {
        WebElement editStoryBtn = waitAndFind(editStoryButton);
        editStoryBtn.click();
    }

    public void clickDeleteStoryButton() {
        WebElement deleteStoryBtn = waitAndFind(deleteStoryButton);
        deleteStoryBtn.click();
    }

    public void clickConfirmDeleteButton() {
        WebElement confirmDeleteBtn = waitAndFind(confirmDeleteButton);
        confirmDeleteBtn.click();
    }

    public void scrollDown() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,1050)");
    }

    public boolean isOnMyStoryPage(String page) {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.urlToBe(baseurl + page));
            return true;
        } catch (TimeoutException te) {
            return false;
        }
    }
}
