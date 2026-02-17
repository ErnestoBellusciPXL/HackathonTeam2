package web.pageObjects;


import org.openqa.selenium.WebDriver;

public class HomePage extends BasePage{
    public HomePage(WebDriver driver) {
        super(driver, "");
    }

    public boolean isOnHomePage() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.equals(baseurl) || 
               currentUrl.equals(baseurl.replaceAll("/$", ""));
    }
}
