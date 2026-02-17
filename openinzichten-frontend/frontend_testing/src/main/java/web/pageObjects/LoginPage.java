package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage extends BasePage {
    private By emailField = By.id("email");
    private By passwordField = By.id("password");
    private By loginButton = By.id("loginButton");
    private By messageToast = By.xpath("//p[@id=\"message-toasts\"]");
    private Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);


    public LoginPage(WebDriver driver) {
        super(driver, "login");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
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

    public void clickLogin() {
        WebElement loginBtn = waitAndFind(loginButton);
        loginBtn.click();
    }

    public String getMessageToast() {
        WebElement toastElem = waitAndFind(messageToast);
        return toastElem.getText();
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

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLogin();
    }

    /**
     * Perform login and wait until either the app navigates to the base URL
     * (indicating success) or a toast message becomes visible (indicating an error).
     * Returns true when navigation to the base URL occurred, false otherwise.
     */
    public boolean loginAndWait(String email, String password, Duration timeout) {
        enterEmail(email);
        enterPassword(password);
        clickLogin();

        try {
            String normalizedBase = normalizedBaseUrl();
            String baseWithoutTrailing = normalizedBase.endsWith("/")
                    ? normalizedBase.substring(0, normalizedBase.length() - 1)
                    : normalizedBase;
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.or(
                            ExpectedConditions.urlToBe(normalizedBase),
                            ExpectedConditions.urlToBe(baseWithoutTrailing),
                            ExpectedConditions.visibilityOfElementLocated(messageToast)
                    )
            );

            String current = driver.getCurrentUrl();
            return current.startsWith(normalizedBase) || current.equals(baseWithoutTrailing);
        } catch (TimeoutException te) {
            return false;
        }
    }
}
