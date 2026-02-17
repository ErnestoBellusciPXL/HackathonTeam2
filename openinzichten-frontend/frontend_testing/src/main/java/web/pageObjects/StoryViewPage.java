package web.pageObjects;

import io.qameta.allure.Allure;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;

public class StoryViewPage extends BasePage {
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    public StoryViewPage(WebDriver driver) {
        super(driver, "stories");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void openStoryByTitle(String title) {
        navigateTo();

        Allure.step("Wacht tot verhalenpagina geladen is", () -> {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.tagName("main")));
        });

        Allure.step("Zoek naar verhaal met titel: " + title, () -> {
            boolean usedSearchInput = false;

            // Probeer eerst de zoekinput te gebruiken
            try {
                By searchInputLocator = By.xpath("//input[@placeholder='Zoek verhalen']");
                WebElement searchInput = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.visibilityOfElementLocated(searchInputLocator));
                searchInput.clear();
                searchInput.sendKeys(title);
                usedSearchInput = true;
            } catch (TimeoutException ignored) {
                // Input niet beschikbaar, fallback naar direct klikken
            }

            By titleLocator = By.xpath("//article[@id='VerhaalGebruikerCard'][contains(normalize-space(.), '" + title + "')]");

            WebElement storyElement = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(titleLocator));

            // Scroll indien nodig
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", storyElement);

            // Klik op het verhaal
            storyElement.click();

            Allure.step("Verhaal " + title + " geopend" + (usedSearchInput ? " via zoekinput" : " direct"));
        });
    }

    public int getStoriesCount() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollBy(0,document.body.scrollHeight)");
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ignored) {
            }
            By articles = By.xpath("//div[contains(@class,'space-y-6')]//article");
            java.util.List<WebElement> els = driver.findElements(articles);
            return els == null ? 0 : els.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getFirstStoryPreviewTitle() {
        try {
            By firstTitle = By.xpath("//div[contains(@class,'space-y-6')]//article[1]//h2");
            WebElement el = new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOfElementLocated(firstTitle));
            return el.getText();
        } catch (Exception e) {
            return "";
        }
    }

    public void filterByCondition(String conditionName) {
        Allure.step("Filter op aandoening: " + conditionName, () -> {
            try {
                // Find the condition select element
                By conditionSelectLocator = By.id("condition-select");
                WebElement conditionSelect = new WebDriverWait(driver, DEFAULT_TIMEOUT)
                        .until(ExpectedConditions.presenceOfElementLocated(conditionSelectLocator));
                
                // Use Selenium's Select class to select by visible text
                Select select = new Select(conditionSelect);
                select.selectByVisibleText(conditionName);
                
                Allure.step("Gefilterd op aandoening: " + conditionName);
            } catch (Exception e) {
                throw new RuntimeException("Kon niet filteren op aandoening: " + conditionName, e);
            }
        });
    }

    public boolean isRecommendedStoriesSectionVisible() {
        try {
            // Look for the "Voorgestelde verhalen" heading (h1 tag)
            By sectionHeading = By.xpath("//h1[contains(text(), 'Voorgestelde verhalen')]");
            WebElement heading = new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.visibilityOfElementLocated(sectionHeading));
            return heading.isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public int getRecommendedStoriesCount() {
        try {
            // Find all article elements within the recommended stories section
            // The section contains an h1 with "Voorgestelde verhalen" text
            By recommendedCards = By.xpath("//h1[contains(text(), 'Voorgestelde verhalen')]/ancestor::section//article");
            java.util.List<WebElement> cards = driver.findElements(recommendedCards);
            return cards.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public java.util.List<String> getRecommendedStoryTitles() {
        try {
            // Find all h2 titles in article elements within the recommended stories section
            // The story cards use h2 for the title, not h3
            By titleLocator = By.xpath("//h1[contains(text(), 'Voorgestelde verhalen')]/ancestor::section//article//h2");
            
            java.util.List<WebElement> titleElements = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(titleLocator));
            
            java.util.List<String> titles = new java.util.ArrayList<>();
            for (WebElement titleElement : titleElements) {
                String title = titleElement.getText().trim();
                if (!title.isEmpty()) {
                    titles.add(title);
                }
            }
            return titles;
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    public String getFirstRecommendedStoryTitle() {
        try {
            // Get all titles and return the first one
            // This is more reliable than using [1] in XPath
            java.util.List<String> titles = getRecommendedStoryTitles();
            if (titles.isEmpty()) {
                return "";
            }
            return titles.get(0);
        } catch (Exception e) {
            return "";
        }
    }
}
