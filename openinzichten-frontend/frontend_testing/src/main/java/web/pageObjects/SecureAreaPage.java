package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SecureAreaPage extends BasePage {
    private By successMessage = By.cssSelector("");

    public SecureAreaPage(WebDriver driver) {
        super(driver, "secure");
    }

    public String getSuccesMessage() {
        WebElement successMessage = driver.findElement(this.successMessage);
        return successMessage.getText();
    }
}
