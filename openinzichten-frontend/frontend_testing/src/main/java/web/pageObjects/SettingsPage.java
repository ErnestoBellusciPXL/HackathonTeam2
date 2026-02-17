package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class SettingsPage extends BasePage {
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(8);
    // Profile form locators
    private final By usernameInput = By.id("new_username");
    private final By emailInput = By.id("new_email");
    private final By zipcodeInput = By.id("zipcode");
    private final By conditionSelectBox = By.cssSelector(".condition-group .select-box");
    private final By conditionDropdown = By.cssSelector(".condition-dropdown");
    private final By hasConditionCheckbox = By.cssSelector("label.checkbox-row input.checkbox");
    private final By chipsRow = By.cssSelector(".chip-row .chip");
    private final By saveButton = By.xpath("//button[normalize-space(.)='Wijzigingen opslaan']");
    private final By restoreButton = By.xpath("//button[normalize-space(.)='Herstellen']");
    private final By tabProfileButton = By.id("profile");
    private final By tabSecurityButton = By.id("security");
    private final By deleteAccountButton = By.id("DeleteAccountButton");
    private final By confirmTextInput = By.id("DeletePasswordInput");
    private final By confirmDeleteButton = By.id("ComfirmDeleteAccountButton");
    private final By navigateToSettingsButton = By.id("NavigateToSettingsButton");
    private final By mobileHamburgerButton = By.xpath("//button[@aria-label='Open menu']");
    private final By mobileSettingsLink = By.xpath("//a[contains(@class,'brand-link-purple-dark-bold') and normalize-space(text())='Instellingen']");
    private final By hoverOverUsername = By.id("HoverUsername");
    private final By logoutButton = By.id("LogOutButton");
    private final By messageToast = By.xpath("//p[@id=\"message-toasts\"]");

    public SettingsPage(WebDriver driver) {
        super(driver, "settings");
    }

    private WebElement waitAndFind(By locator) {
        try {
            return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException te) {
            // Provide more context for debugging: current URL and a short page source preview
            try {
                String current = driver.getCurrentUrl();
                String src = driver.getPageSource();
                int len = Math.min(src.length(), 2000);
                System.out.println("waitAndFind timeout for locator: " + locator.toString());
                System.out.println("Current URL: " + current);
                System.out.println("Page source preview:\n" + src.substring(0, len));
            } catch (Exception ignored) {}
            throw te;
        }
    }

    private WebElement waitAndFindClickable(By locator) {
        try {
            return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException te) {
            // Provide more context for debugging: current URL and a short page source preview
            try {
                String current = driver.getCurrentUrl();
                String src = driver.getPageSource();
                int len = Math.min(src.length(), 2000);
                System.out.println("waitAndFindClickable timeout for locator: " + locator.toString());
                System.out.println("Current URL: " + current);
                System.out.println("Page source preview:\n" + src.substring(0, len));
            } catch (Exception ignored) {}
            throw te;
        }
    }

    public boolean isToastMessagePresent(String message) {
        try {
            // Haal alle zichtbare toasts op dat moment op
            List<WebElement> toasts = driver.findElements(messageToast);

            for (WebElement toast : toasts) {
                if (toast.getText().contains(message)) {
                    System.out.println(toast.getText());
                    return true; // Gevonden!
                }
            }
        } catch (org.openqa.selenium.TimeoutException e) {
            // Geen enkele toast verscheen binnen de wachttijd
            return false;
        }

        return false;
    }

    private void clickDeleteAccount() {
        WebElement deleteAccountBtn = waitAndFind(deleteAccountButton);
        deleteAccountBtn.click();
    }

    private void enterConfirmText(String password) {
        WebElement confirmTextElem = waitAndFindClickable(confirmTextInput);
        confirmTextElem.clear();
        confirmTextElem.sendKeys(password);
    }

    private void clickConfirmDelete() {
        WebElement confirmDeleteBtn = waitAndFind(confirmDeleteButton);
        confirmDeleteBtn.click();
    }

    public void hoverOverUsername() {
        WebElement hoverElem = waitAndFind(hoverOverUsername);
        Actions action = new Actions(driver);
        action.moveToElement(hoverElem).perform();
    }

    public void clickOnUsername() {
        WebElement hoverElem = waitAndFind(hoverOverUsername);
        hoverElem.click();
    }

    public void navigateToSettings() {
        try {
            if (driver.getCurrentUrl() != null && driver.getCurrentUrl().contains("/settings")) {
                return;
            }
        } catch (Exception ignored) {}

        // Try desktop dropdown (requires hover)
        try {
            hoverOverUsername();
            WebElement navigateBtn = waitAndFindClickable(navigateToSettingsButton);
            navigateBtn.click();
            return;
        } catch (Exception ignored) {}

        // Fallback: mobile hamburger menu
        try {
            WebElement burger = waitAndFindClickable(mobileHamburgerButton);
            burger.click();
            WebElement mobileLink = waitAndFindClickable(mobileSettingsLink);
            mobileLink.click();
            return;
        } catch (Exception ignored) {}

        // Last resort: direct navigation
        this.navigateTo();
    }

    public void clickLogout() {
        WebElement logoutBtn = waitAndFind(logoutButton);
        logoutBtn.click();
    }

    public boolean isUserLoggedIn() {
        try {
            waitAndFind(hoverOverUsername);
            return true;
        }
        catch (TimeoutException te) {
            return false;
        }
    }

    public void logout() {
        // Open user menu, navigate to settings, then click logout for reliability
        try {
            hoverOverUsername();
            navigateToSettings();
            clickLogout();
        } catch (TimeoutException te) {
            System.out.println("Failed to logout via settings flow, attempting direct settings navigation: " + te.getMessage());
            // Fallback: navigate directly to settings page and retry
            try {
                this.navigateTo();
                clickLogout();
            } catch (TimeoutException te2) {
                System.out.println("Fallback logout failed: " + te2.getMessage());
                throw te2;
            }
        }
    }

    public void deleteAccount(String password) {
        // Ensure we are on settings page and on the Security tab first
        navigateToSecurityTab();
        clickSecurityTab();
        clickDeleteAccount();
        enterConfirmText(password);
        clickConfirmDelete();
    }

    /**
     * Attempts to delete the account and waits until a toast with the provided
     * message appears (or until timeout). Returns true when the toast was seen.
     */
    public boolean deleteAccountAndWait(String password, String message) {
        // Ensure we are on settings page and on the Security tab first
        navigateToSecurityTab();
        clickSecurityTab();
        clickDeleteAccount();
        enterConfirmText(password);
        clickConfirmDelete();

        // Wait up to DEFAULT_TIMEOUT for the toast message to appear
        long end = System.currentTimeMillis() + DEFAULT_TIMEOUT.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                if (isToastMessagePresent(message)) return true;
            } catch (Exception ignored) {}
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        return false;
    }

    public boolean isOnSettingsPage(String page) {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.urlToBe(baseurl + page));
            return true;
        } catch (TimeoutException te) {
            return false;
        }
    }

    // ===== Profile form helpers =====
    public void setUsername(String value) {
        WebElement el = waitAndFindClickable(usernameInput);
        el.clear();
        el.sendKeys(value);
    }

    public String getUsername() {
        WebElement el = waitAndFindClickable(usernameInput);
        return el.getAttribute("value");
    }

    public void setEmail(String value) {
        WebElement el = waitAndFindClickable(emailInput);
        el.clear();
        el.sendKeys(value);
    }

    public String getEmail() {
        WebElement el = waitAndFindClickable(emailInput);
        return el.getAttribute("value");
    }

    public void openZipcodeDropdown() {
        WebElement el = waitAndFindClickable(zipcodeInput);
        el.click();
    }

    public void selectZipcodeByCode(String code) {
        // Opens dropdown and selects the first item containing the code in its label
        openZipcodeDropdown();
        By itemBy = By.xpath("//ul[contains(@class,'scroll-stable')]//li[contains(normalize-space(.), '" + code + "')]");
        WebElement item = waitAndFindClickable(itemBy);
        item.click();
    }

    public String getZipcodeDisplayedLabel() {
        WebElement el = waitAndFindClickable(zipcodeInput);
        return el.getAttribute("value");
    }

    public void selectFirstZipcodeSuggestionDifferentFrom(String previousLabel) {
        openZipcodeDropdown();
        // Wait for suggestion items to appear
        By itemsBy = By.xpath("//ul[contains(@class,'scroll-stable')]//li");
        List<WebElement> items = driver.findElements(itemsBy);
        long end = System.currentTimeMillis() + DEFAULT_TIMEOUT.toMillis();
        while (items.isEmpty() && System.currentTimeMillis() < end) {
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            items = driver.findElements(itemsBy);
        }

        // Try clicking each suggestion (using clickable wait) until the displayed
        // zipcode label differs from the previous label, or until timeout.
        for (int i = 0; i < items.size(); i++) {
            By indexedBy = By.xpath("(//ul[contains(@class,'scroll-stable')]//li)[" + (i + 1) + "]");
            try {
                WebElement candidate = waitAndFindClickable(indexedBy);
                candidate.click();

                // Wait a short time for the input value to update
                long waitEnd = System.currentTimeMillis() + DEFAULT_TIMEOUT.toMillis();
                while (System.currentTimeMillis() < waitEnd) {
                    String current = getZipcodeDisplayedLabel();
                    if (current != null && !current.equals(previousLabel) && !current.isEmpty()) {
                        return;
                    }
                    try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                }
                // if not changed, continue to next candidate
            } catch (Exception ignored) {
                // try next
            }
        }

        // Final fallback: try clicking the first item directly
        if (!items.isEmpty()) {
            try {
                WebElement first = waitAndFindClickable(itemsBy);
                first.click();
            } catch (Exception ignored) {}
        }
    }

    public void openConditionDropdown() {
        WebElement box = waitAndFindClickable(conditionSelectBox);
        box.click();
        // Ensure dropdown rendered
        waitAndFind(conditionDropdown);
    }

    public void addConditionByName(String name) {
        openConditionDropdown();
        By pillBy = By.xpath("//div[contains(@class,'condition-dropdown')]//button[contains(@class,'pill') and normalize-space(.)='" + name + "']");
        WebElement pill = waitAndFindClickable(pillBy);
        pill.click();
    }

    public void removeConditionByName(String name) {
        By chipClose = By.xpath("//span[contains(@class,'chip')][normalize-space(.)='" + name + "']//button[contains(@class,'chip-close')]");
        WebElement closeBtn = waitAndFindClickable(chipClose);
        closeBtn.click();
    }

    public List<WebElement> getSelectedConditionChips() {
        return driver.findElements(chipsRow);
    }

    public void setHasCondition(boolean value) {
        WebElement checkbox = waitAndFindClickable(hasConditionCheckbox);
        boolean checked = checkbox.isSelected();
        if (checked != value) {
            checkbox.click();
        }
    }

    public boolean isHasConditionChecked() {
        WebElement checkbox = waitAndFindClickable(hasConditionCheckbox);
        return checkbox.isSelected();
    }

    public void clickSaveChanges() {
        WebElement btn = null;
        try {
            btn = waitAndFindClickable(saveButton);
        } catch (Exception ignored) {
            // Element not clickable (possibly disabled). Try to find it anyway to inspect state.
            try { btn = waitAndFind(saveButton); } catch (Exception e) { /* ignore */ }
        }

        if (btn == null) {
            System.out.println("Save button not found (clickSaveChanges)");
            return;
        }

        try {
            System.out.println("Save button displayed=" + btn.isDisplayed() + " enabled=" + btn.isEnabled() + " disabledAttr=" + btn.getAttribute("disabled"));
        } catch (Exception ignored) {}

        try {
            btn.click();
            return;
        } catch (Exception e) {
            // Fallback: use JS click
            try {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                return;
            } catch (Exception ignored) {
                System.out.println("clickSaveChanges: failed to click save button: " + e.getMessage());
            }
        }
    }

    public void clickRestoreChanges() {
        WebElement btn = waitAndFindClickable(restoreButton);
        btn.click();
    }

    public void clickProfileTab() {
        try {
            WebElement btn = waitAndFindClickable(tabProfileButton);
            btn.click();
        } catch (Exception ignored) {}
    }

    public void clickSecurityTab() {
        try {
            WebElement btn = waitAndFindClickable(tabSecurityButton);
            btn.click();
        } catch (Exception ignored) {}
    }

    public void navigateToSecurityTab() {
        String normalizedBase = normalizedBaseUrl();
        String fullUrl = normalizedBase + "settings?tab=security";
        driver.navigate().to(fullUrl);
    }
}