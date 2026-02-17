package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ConnectionsPage extends BasePage {
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final By deleteConnectionButton = By.id("DeleteConnectionButton");
    private final By chatOpenWithConnection = By.id("ChatWithConnection");
    private final By requestsTab = By.xpath("//button[contains(normalize-space(.), 'Verzoeken')]");
    private final By navLink = By.xpath("//a[@href='/connections' or contains(@href, '/connections')]");
    private final By acceptBtn = By.xpath(".//button[normalize-space() = 'Accepteer']");
    private final By connectionsTab = By.xpath("//button[contains(normalize-space(.), 'Connecties')]");

    public ConnectionsPage(WebDriver driver) {
        super(driver, "connections");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private Boolean waitAndFindInvisible(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    private WebElement waitAndFindClickable(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void navigateToRequestsTab() {
        navigateTo();
        // Click the 'Verzoeken' main tab
        // Use contains() to match the button that includes the count: "Verzoeken (N)"
        try {
            WebElement el = waitAndFind(requestsTab);
            el.click();
        } catch (Exception ignored) {
            // If not found, assume already on requests tab
        }
    }

    /**
     * Navigate to the connections page using the top navigation bar link,
     * then switch to the 'Verzoeken' tab. This mirrors user flow via the navbar.
     */
    public void navigateToRequestsViaNav() {
        try {
            // Find a nav link that points to the connections route and click it
            WebElement link = waitAndFindClickable(navLink);
            link.click();
        } catch (Exception e) {
            // Fallback to direct navigation if nav link isn't present
            navigateTo();
        }
        // Then ensure the requests tab is selected by clicking the in-page button
        // The button text contains the word 'Verzoeken' and the count, e.g. "Verzoeken (1)"
        try {
            WebElement el = waitAndFind(requestsTab);
            el.click();
        } catch (Exception ignored) {
            // If clicking fails, nothing more to do here
        }
    }

    // (removed unused helper) requestBlockLocator() is used directly by accept/decline

    private By requestBlockLocator(String username) {
        return By.xpath("//div[contains(@class,'border')][.//div[contains(normalize-space(.), '" + username + "')]]");
    }

    public boolean acceptRequestForUser(String username) {
        try {
            By blockLoc = requestBlockLocator(username);
            WebElement block = waitAndFind(blockLoc);
            WebElement acceptBtn = block.findElement(this.acceptBtn);
            acceptBtn.click();
            // Wait until the request card disappears (request processed)
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.invisibilityOfElementLocated(blockLoc));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean declineRequestForUser(String username) {
        try {
            By blockLoc = requestBlockLocator(username);
            WebElement block = waitAndFind(blockLoc);
            WebElement declineBtn = block.findElement(By.xpath(".//button[normalize-space() = 'Weiger']"));
            declineBtn.click();
            // Wait until the request card disappears
            waitAndFindInvisible(blockLoc);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click the main "Connecties" tab button on the connections page.
     */
    public void navigateToConnectionsTab() {
        navigateTo();
        try {
            WebElement el = waitAndFind(connectionsTab);
            el.click();
        } catch (Exception ignored) {
            // If not found, assume already on connections tab
        }
    }

    /**
     * Click the chat button for a connection in the connections list.
     */
    public void openChatForUser(String username) {
        try {
            By chatBtn = By.xpath("//div[contains(@class,'border')][.//div[contains(normalize-space(.), '" + username + "')]]//button[normalize-space() = 'Chat']");
            WebElement el = waitAndFindClickable(chatBtn);
            el.click();
            // wait for chat input to appear
            By input = By.cssSelector("input[placeholder='Typ een bericht...']");
            waitAndFind(input);
        } catch (Exception ignored) {
        }
    }

    /**
     * Return true if the given username appears in the connections list.
     */
    public boolean isUserInConnections(String username) {
        try {
            By locator = By.xpath("//div[contains(@class,'font-semibold') and contains(normalize-space(.), '" + username + "')]");
            WebElement el = waitAndFind(locator);
            return el != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void clickDeleteConnection() {
        WebElement deleteConnectionBtn = waitAndFind(deleteConnectionButton);
        deleteConnectionBtn.click();
    }

    public boolean isChatOpenWithConnection() {
        try {
            WebElement chatElem = waitAndFind(chatOpenWithConnection);
            return chatElem != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isOnConnectionsPage(String page) {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.urlToBe(baseurl + page));
            return true;
        } catch (TimeoutException te) {
            return false;
        }
    }
}
