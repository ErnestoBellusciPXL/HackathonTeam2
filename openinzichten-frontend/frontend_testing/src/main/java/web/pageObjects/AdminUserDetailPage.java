package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class AdminUserDetailPage extends BasePage {
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(8);
    
    private final By usernameField = By.id("username");
    private final By emailField = By.id("email");
    private final By conditionItems = By.cssSelector(".condition-item");
    private final By userStoryTitle = By.id("user-story-title");
    private final By userStory = By.id("user-story");
    private final By statusBadge = By.id("status");
    private final By toggleUserActivationButton = By.id("toggle-user-activation");
    private final By backButton = By.cssSelector("button[aria-label='Terug']");

    public AdminUserDetailPage(WebDriver driver) {
        super(driver, "admin/users");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    
    private List<WebElement> waitAndFindAll(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    public String getUsername() {
        try {
            WebElement element = waitAndFind(usernameField);
            return element.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String getEmail() {
        try {
            WebElement element = waitAndFind(emailField);
            return element.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public int getConditionCount() {
        try {
            List<WebElement> conditions = waitAndFindAll(conditionItems);
            return conditions.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isUserStoryTitlePresent() {
        try {
            WebElement element = waitAndFind(userStoryTitle);
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isUserStoryPresent() {
        try {
            WebElement element = waitAndFind(userStory);
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isStatusPresent() {
        try {
            WebElement element = waitAndFind(statusBadge);
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isToggleUserActivationButtonPresent() {
        try {
            WebElement element = waitAndFind(toggleUserActivationButton);
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickToggleUserActivation() {
        try {
            WebElement button = waitAndFind(toggleUserActivationButton);
            button.click();
        } catch (Exception e) {
            // Handle case where button is not available
        }
    }

    public void clickBack() {
        try {
            WebElement button = waitAndFind(backButton);
            button.click();
        } catch (Exception e) {
            // Handle case where button is not available
        }
    }

    public boolean isOnUserDetailPage() {
        try {
            String currentUrl = driver.getCurrentUrl();
            return currentUrl != null && currentUrl.contains("/admin/users/");
        } catch (Exception e) {
            return false;
        }
    }
}
