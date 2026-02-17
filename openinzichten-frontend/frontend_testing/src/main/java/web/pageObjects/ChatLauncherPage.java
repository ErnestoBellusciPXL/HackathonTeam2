package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ChatLauncherPage extends BasePage {
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(6);
    private final By chatsOverview = By.id("ChatsOverview");
    private final By header = By.xpath("//div[contains(normalize-space(.), 'Chats')]");
    private final By input = By.cssSelector("input[placeholder='Typ een bericht...']");
    private final By sendBtn = By.xpath("//button[normalize-space() = 'Stuur']");
    private final By chatOpenWithConnection = By.id("ChatWithConnection");

    public ChatLauncherPage(WebDriver driver) {
        super(driver, "");
    }

    public void openLauncher() {
        try {
            By launcherBtn = By.cssSelector("button[aria-label='Open chats']");
            WebElement el = new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(launcherBtn));
            el.click();
        } catch (Exception ignored) {
        }
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private WebElement waitAndFindClickable(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void openOverview() {
        try {
            waitAndFind(header);
        } catch (Exception ignored) {
        }
    }

    public void openChatByUsername(String username) {
        try {
            By userItem = By.xpath("//div[contains(@class,'font-medium') and normalize-space() = '" + username + "']/ancestor::li");
            WebElement el = waitAndFind(userItem);
            el.click();
            // wait for chat input to appear
            waitAndFind(input);
        } catch (Exception ignored) {
        }
    }

    public void sendMessageInOpenChat(String message) {
        try {
            By input = By.cssSelector("input[placeholder='Typ een bericht...']");
            WebElement in = waitAndFind(input);
            // Use JS to set value and dispatch input event so Vue's v-model updates reliably
            ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));", in, message);
            // Verify the input actually contains the message (trim) before clicking send
            String current = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].value;", in);
            if (current == null || current.trim().length() == 0) {
                throw new RuntimeException("Input not populated after JS set");
            }
            WebElement btn = waitAndFindClickable(sendBtn);
            // Ensure button is enabled
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(d -> {
                String disabled = btn.getAttribute("disabled");
                return disabled == null || !"true".equals(disabled);
            });
            btn.click();
            // wait for message to appear in the list
            By msgLoc = By.xpath("//div[contains(@class,'text-sm') and contains(normalize-space(.), '" + message + "')]");
            waitAndFind(msgLoc);
        } catch (Exception ignored) {
        }
    }

    public boolean isMessageVisibleInOpenChat(String message) {
        try {
            By msgLoc = By.xpath("//div[contains(@class,'text-sm') and contains(normalize-space(.), '" + message + "')]");
            WebElement el = waitAndFind(msgLoc);
            return el != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isChatsOverviewVisible() {
        try {
            WebElement chatsOverviewElem = waitAndFind(chatsOverview);
            return chatsOverviewElem != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isChatOpenWithConnection() {
        try {
            WebElement chatElem = waitAndFind(chatOpenWithConnection);
            return chatElem != null;
        } catch (Exception e) {
            return false;
        }
    }
}
