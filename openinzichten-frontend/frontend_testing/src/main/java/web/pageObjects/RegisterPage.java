package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.Keys;
import java.util.List;
import java.time.Duration;

public class RegisterPage extends BasePage {
    // registratie stap 1
    private By usernameField = By.id("username");
    private By emailField = By.id("email");
    private By passwordField = By.id("password");
    private By confirmPasswordField = By.id("passwordRepeat");
    private By termsCheckbox = By.id("terms");
    private By registerNextButton = By.id("loginButton");

    // registratie stap 2
    private By postalCodeSearch = By.id("zipcode");
    private By conditionSearch = By.id("conditionQuery");
    private By hasConditionCheckbox = By.id("hasCondition");
    private By completeProfileButton = By.id("CompleteProfileButton");

    // info tooltips
    private By usernameInfoIcon = By.cssSelector("#username ~ span a[aria-describedby='info-tooltip']");
    private By passwordInfoIcon = By.cssSelector("#password ~ span a[aria-describedby='info-tooltip']");
    private By zipcodeInfoIcon = By.cssSelector("#zipcode ~ div a[aria-describedby='info-tooltip']");
    private By conditionInfoIcon = By.cssSelector("#conditionQuery ~ span a[aria-describedby='info-tooltip']");

    private Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private By messageToast = By.xpath("//p[@id=\"message-toasts\"]");


    public RegisterPage(WebDriver driver) {
        super(driver, "register");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void enterUsername(String username) {
        WebElement usernameElem = waitAndFind(usernameField);
        usernameElem.sendKeys(username);
    }

    public void enterEmail(String email) {
        WebElement emailElem = waitAndFind(emailField);
        emailElem.sendKeys(email);
    }

    public void enterPassword(String password) {
        WebElement passwordElem = waitAndFind(passwordField);
        passwordElem.clear();
        passwordElem.sendKeys(password);
    }

    public void enterConfirmPassword(String password) {
        WebElement confirmPasswordElem = waitAndFind(confirmPasswordField);
        confirmPasswordElem.clear();
        confirmPasswordElem.sendKeys(password);
    }

    public void checkTerms() {
        WebElement termsElem = waitAndFind(termsCheckbox);
        if (!termsElem.isSelected()) {
            termsElem.click();
        }
    }

    public void clickRegisterNext() {
        WebElement registerBtn = waitAndFind(registerNextButton);
        // Ensure button is clickable before clicking
        new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(registerNextButton));
        registerBtn.click();
        // Small delay to allow form validation and next step to start
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
    }

    public void enterPostalCode(String postalCode) {
        WebElement postalCodeElem = waitAndFind(postalCodeSearch);
        // ensure input is focused, clear and type
        postalCodeElem.click();
        postalCodeElem.clear();
        postalCodeElem.sendKeys(postalCode);
        // wait for the dropdown to appear and try to select the matching suggestion
        try {
            By dropdownLocator = By.cssSelector("ul.z-10, ul[class*='z-10']");
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            shortWait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

            List<WebElement> items = driver.findElements(By.cssSelector("ul.z-10 li, ul[class*='z-10'] li"));
            WebElement match = null;
            for (WebElement it : items) {
                String txt = it.getText();
                if (txt == null) continue;
                String normalized = txt.trim();
                if (normalized.contains(postalCode) || normalized.endsWith("(" + postalCode + ")") || normalized.startsWith(postalCode)) {
                    match = it;
                    break;
                }
            }

            if (match != null) {
                shortWait.until(ExpectedConditions.elementToBeClickable(match)).click();
            } else if (!items.isEmpty()) {
                // Fallback: press arrow down + enter to pick the first suggestion
                postalCodeElem.sendKeys(Keys.ARROW_DOWN);
                postalCodeElem.sendKeys(Keys.ENTER);
            }
        } catch (Exception ignored) {
            // ignore: suggestion may not appear in this environment
        }
    }

    public void enterCondition(String condition) {
        WebElement conditionElem = waitAndFind(conditionSearch);
        conditionElem.sendKeys(condition);
    }

    public void selectCondition(String condition) {
        WebElement selectConditionElem = waitAndFind(By.xpath("//button[@id='FilterConditionButton' and contains(normalize-space(.),'" + condition + "')]"));
        selectConditionElem.click();
    }

    public void checkHasCondition() {
        WebElement hasConditionElem = waitAndFind(hasConditionCheckbox);
        if (!hasConditionElem.isSelected()) {
            hasConditionElem.click();
        }
    }

    public void clickCompleteProfile() {
        WebElement completeProfileBtn = waitAndFind(completeProfileButton);
        completeProfileBtn.click();
    }

    public String getMessageToast() {
        try {
            // Wait up to the default timeout for a visible toast with non-empty text.
            String text = new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(d -> {
                        try {
                            WebElement el = d.findElement(messageToast);
                            if (el != null && el.isDisplayed()) {
                                String t = el.getText();
                                if (t != null && !t.trim().isEmpty()) return t.trim();
                            }
                        } catch (Exception ignored) {
                            // ignored: element may not yet be present or visible
                        }
                        return null;
                    });

            return text != null ? text : "";
        } catch (TimeoutException te) {
            return "";
        }
    }

    public boolean isOnHomePage() {
        try {
            String normalizedBase = normalizedBaseUrl();
            String baseWithoutTrailing = normalizedBase.endsWith("/")
                    ? normalizedBase.substring(0, normalizedBase.length() - 1)
                    : normalizedBase;
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.or(
                            ExpectedConditions.urlToBe(normalizedBase),
                            ExpectedConditions.urlToBe(baseWithoutTrailing)
                    ));
            return true;
        } catch (TimeoutException te) {
            return false;
        }
    }

    /**
     * Returns true if the second registration form (postal code / conditions)
     * is currently visible.
     */
    public boolean isOnSecondForm() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(1))
                    .until(ExpectedConditions.visibilityOfElementLocated(postalCodeSearch));
            return true;
        } catch (TimeoutException te) {
            return false;
        }
    }

    public boolean waitForSecondForm() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOfElementLocated(postalCodeSearch));
            return true;
        } catch (TimeoutException te) {
            return false;
        }
    }

    public void registerStepOne(String username, String email, String password, String passwordConfirm) {
        enterUsername(username);
        enterEmail(email);
        enterPassword(password);
        enterConfirmPassword(passwordConfirm);
        checkTerms();
        clickRegisterNext();
        // wait for second form (zipcode input) to become visible; sometimes the transition
        // and data loading can take longer than the default timeout used elsewhere.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(postalCodeSearch));
        } catch (TimeoutException ignored) {
            // let subsequent steps handle the visibility check and potentially fail with
            // a clearer error; swallowing here avoids throwing from registration step one
        }
    }

    public void registerStepTwo(String postalCode, String condition) {
        enterPostalCode(postalCode);
        enterCondition(condition);
        selectCondition(condition);
        checkHasCondition();
        clickCompleteProfile();

        // Wait until registration completes and the app either shows success toast
        // or navigates back to the home page (token set and logged in).
        try {
            String normalizedBase = normalizedBaseUrl();
            String baseWithoutTrailing = normalizedBase.endsWith("/")
                    ? normalizedBase.substring(0, normalizedBase.length() - 1)
                    : normalizedBase;

            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
                try {
                    String current = d.getCurrentUrl();
                    if (current.equals(normalizedBase) || current.equals(baseWithoutTrailing)) return true;
                    WebElement toast = d.findElement(messageToast);
                    if (toast != null && toast.isDisplayed()) {
                        String t = toast.getText();
                        if (t != null && t.contains("Instellingen zijn opgeslagen.")) return true;
                    }
                } catch (Exception ignored) {}
                return false;
            });
        } catch (TimeoutException ignored) {
            // Non-fatal: subsequent steps may still succeed, but Settings navigation
            // requires auth; tests that depend on immediate auth should handle it.
        }
    }

    public String getUsernameVisibilityInfoText() {
        By infoTextLocator = By.id("username_public_info");
        WebElement infoTextElem = waitAndFind(infoTextLocator);
        return infoTextElem.getText().trim();
    }

    public String getEmailVisibilityInfoText() {
        By infoTextLocator = By.id("email_private_info");
        WebElement infoTextElem = waitAndFind(infoTextLocator);
        return infoTextElem.getText().trim();
    }

    private String hoverTooltipText(By iconLocator) {
        WebElement icon = waitAndFind(iconLocator);
        WebElement tooltip = icon.findElement(By.cssSelector("div[role='tooltip']"));

        new Actions(driver).moveToElement(icon).perform();
        new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.attributeToBe(tooltip, "aria-hidden", "false"));

        return tooltip.getText().trim();
    }

    public String getUsernameTooltipText() {
        return hoverTooltipText(usernameInfoIcon);
    }

    public String getPasswordTooltipText() {
        return hoverTooltipText(passwordInfoIcon);
    }

    public String getZipcodeTooltipText() {
        return hoverTooltipText(zipcodeInfoIcon);
    }

    public String getConditionTooltipText() {
        return hoverTooltipText(conditionInfoIcon);
    }
}
