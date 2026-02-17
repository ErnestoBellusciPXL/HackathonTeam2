package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.List;
import java.util.ArrayList;
import java.time.Duration;
import java.util.regex.Pattern;

public class WriteStoryPage extends BasePage {
    private By titleField = By.id("TitleInput");
    private By storyField = By.id("StoryInput");
    private By submitButton = By.id("SubmitStoryButton");
    private By cancelStoryButton = By.id("CancelStoryButton");
    private By navigateToWriteStoryButton = By.xpath("//a[@id='NavigateToMyStoryButton']");
    private By mobileHamburgerButton = By.xpath("//button[@aria-label='Open menu']");
    private By mobileMijnVerhaalLink = By.xpath("//a[contains(text(),'Mijn Verhaal')]");
    private By hoverOverUsername = By.id("HoverUsername");
    private By messageToast = By.xpath("//p[@id=\"message-toasts\"]");
    private Duration defaultTimeout = Duration.ofSeconds(10);

    public WriteStoryPage(WebDriver driver) {
        super(driver, "write");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, defaultTimeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private WebElement waitAndFindClickable(By locator) {
        try {
            return new WebDriverWait(driver, defaultTimeout)
                    .until(ExpectedConditions.elementToBeClickable(locator));
        } catch (org.openqa.selenium.TimeoutException te) {
            // Fallback: element might be visible but overlapped; try visibility only
            return new WebDriverWait(driver, defaultTimeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
        }
    }

    public void enterTitle(String title) {
        WebElement titleElem = waitAndFindClickable(titleField);
        if (titleElem != null) {
            titleElem.clear();
            titleElem.sendKeys(title);
        }
    }
    public void enterStory(String story) {
        WebElement storyElem = waitAndFindClickable(storyField);
        if (storyElem != null) {
            try {
                // ensure the element is scrolled into view to avoid overlap issues
                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].scrollIntoView({block: 'center'});", storyElem);
            } catch (Exception ignored) {}
            try {
                storyElem.clear();
            } catch (Exception ignored) {
                // If clear fails (overlap), try JS set value
                try {
                    ((JavascriptExecutor) driver)
                            .executeScript("arguments[0].value='';", storyElem);
                } catch (Exception ignored2) {}
            }
            try {
                storyElem.sendKeys(story);
            } catch (Exception e) {
                // As a last resort, focus via JS and input via Actions
                try {
                    ((JavascriptExecutor) driver)
                            .executeScript("arguments[0].focus();", storyElem);
                    new Actions(driver).moveToElement(storyElem).click().sendKeys(story).perform();
                } catch (Exception ignored) {}
            }
        }
    }
    /**
     * Wait until a condition is visibly selected for the user or until the placeholder
     * "Nog geen aandoeningen gekozen." is gone. This prevents races where the test
     * submits before the frontend has applied the user's single-linked condition.
     */
    public void waitForConditionSelection() {
        new WebDriverWait(driver, defaultTimeout).until(d -> {
            try {
                // selected chips have class `bg-brand-purple`
                if (!d.findElements(By.cssSelector("span.bg-brand-purple")).isEmpty()) return true;

                // if the placeholder text 'Nog geen aandoeningen gekozen.' is not present,
                // assume selection has occurred (or there are no conditions to choose).
                List<WebElement> placeholders = d.findElements(By.xpath("//span[contains(text(),'Nog geen aandoeningen gekozen')]") );
                return placeholders.isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Wait until any existing message toast is gone or empty so we can reliably
     * capture the toast produced by the next action.
     */
    public void waitForNoMessageToast() {
        new WebDriverWait(driver, defaultTimeout).until(d -> {
            try {
                List<WebElement> els = d.findElements(messageToast);
                if (els.isEmpty()) return true;
                String t = els.get(0).getText();
                return t == null || t.trim().isEmpty();
            } catch (Exception e) {
                return true;
            }
        });
    }
    public void clickSubmit() {
        WebElement submitBtn = waitAndFindClickable(submitButton);
        if (submitBtn != null) {
            submitBtn.click();
        }
    }
    public void clickCancel() {
        WebElement cancelBtn = waitAndFindClickable(cancelStoryButton);
        if (cancelBtn != null) {
            cancelBtn.click();
        }
    }

    public void navigateToWriteStory() {
        // The menu item lives in a hover dropdown; ensure it's shown first.
        try { hoverOverUsername(); } catch (Exception ignored) {}
        try {
            WebElement navigateBtn = waitAndFindClickable(navigateToWriteStoryButton);
            if (navigateBtn != null) {
                navigateBtn.click();
                return;
            }
        } catch (Exception ignored) {}

        // Fallback for mobile layout: open hamburger and tap "Mijn Verhaal"
        try {
            WebElement burger = waitAndFindClickable(mobileHamburgerButton);
            if (burger != null) burger.click();
            WebElement mobileLink = waitAndFindClickable(mobileMijnVerhaalLink);
            if (mobileLink != null) mobileLink.click();
        } catch (Exception ignored) {}
    }

    public void hoverOverUsername() {
        WebElement hoverElem = waitAndFind(hoverOverUsername);
        if (hoverElem != null) {
            Actions action = new Actions(driver);
            action.moveToElement(hoverElem).perform();
        }
    }

    public boolean isOnMyStoryPage() {
        try {
            new WebDriverWait(driver, defaultTimeout)
                    .until(ExpectedConditions.urlContains("my-story"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isOnHomePage() {
        try {
            // Accept the base URL with or without a trailing slash.
            String regex = Pattern.quote(baseurl) + "/?";
            new WebDriverWait(driver, defaultTimeout)
                    .until(ExpectedConditions.urlMatches(regex));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getMessageToast() {
        try {
            return new WebDriverWait(driver, defaultTimeout)
                    .until(d -> {
                        try {
                            List<WebElement> els = d.findElements(messageToast);
                            List<String> msgs = new ArrayList<>();
                            for (WebElement el : els) {
                                if (el != null && el.isDisplayed()) {
                                    String t = el.getText();
                                    if (t != null && !t.trim().isEmpty()) msgs.add(t.trim());
                                }
                            }
                            if (!msgs.isEmpty()) return String.join("\n", msgs);
                        } catch (Exception ignored) {
                            // toast may not be present yet
                        }
                        return null;
                    });
        } catch (Exception e) {
            // fallback: try to collect whatever toasts are present right now
            try {
                List<WebElement> els = driver.findElements(messageToast);
                List<String> msgs = new ArrayList<>();
                for (WebElement el : els) {
                    try {
                        if (el != null && el.isDisplayed()) {
                            String t = el.getText();
                            if (t != null && !t.trim().isEmpty()) msgs.add(t.trim());
                        }
                    } catch (Exception ignored) {}
                }
                return msgs.isEmpty() ? "" : String.join("\n", msgs);
            } catch (Exception ignored) {
                return "";
            }
        }
    }


    public void writeStory(String title, String story) {
        // ensure previous toasts are cleared and any auto-selection of a single
        // user condition has completed before interacting with the form.
        waitForNoMessageToast();
        waitForConditionSelection();
        enterStory(story);
        enterTitle(title);
        clickSubmit();
    }

    public void cancelStory(String title, String story) {
        enterStory(story);
        enterTitle(title);
        clickCancel();
    }

}