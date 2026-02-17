package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class StoryDetailPage extends BasePage {
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final By reportStoryButton = By.id("RapporteerVerhaalButton");
    private final By reportReasonDropdown = By.id("report-reason");
    private final By optionSpam = By.xpath("//option[@value='SPAM']");
    private final By optionHarassment = By.xpath("//option[@value='HARASSMENT']");
    private final By optionInappropriateContent = By.xpath("//option[@value='INAPPROPRIATE_CONTENT']");
    private final By optionMisinformation = By.xpath("//option[@value='MISINFORMATION']");
    private final By optionOther = By.xpath("//option[@value='OTHER']");
    private final By otherReasonInput = By.id("other-reason");
    private final By sendReport = By.id("Rapporteer");
    private final By cancelReport = By.id("AnnuleerButton");
    private final By messageToast = By.xpath("//p[@id=\"message-toasts\"]");

    public StoryDetailPage(WebDriver driver) {
        super(driver, "story");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void clickReportStory() {
        WebElement reportBtn = waitAndFind(reportStoryButton);
        reportBtn.click();
    }

    public void clickReportReason() {
        WebElement dropdown = waitAndFind(reportReasonDropdown);
        dropdown.click();
    }

    public void selectOption(String option) {
        By optionSelector;
        switch (option.toUpperCase()) {
            case "SPAM":
                optionSelector = optionSpam;
                break;
            case "HARASSMENT":
                optionSelector = optionHarassment;
                break;
            case "INAPPROPRIATE_CONTENT":
                optionSelector = optionInappropriateContent;
                break;
            case "MISINFORMATION":
                optionSelector = optionMisinformation;
                break;
            case "OTHER":
                optionSelector = optionOther;
                break;
            default:
                throw new IllegalArgumentException("Invalid option: " + option);
        }
        WebElement optionElem = waitAndFind(optionSelector);
        optionElem.click();
    }

    public void enterOtherReason(String reason) {
        WebElement otherReasonElem = waitAndFind(otherReasonInput);
        otherReasonElem.clear();
        otherReasonElem.sendKeys(reason);
    }

    public void clickSendReport() {
        WebElement sendReportBtn = waitAndFind(sendReport);
        sendReportBtn.click();
    }

    public String getStoryTitle() {
        By titleSel = By.id("story-title");
        WebElement el = waitAndFind(titleSel);
        return el.getText();
    }

    public String getStoryContent() {
        By contentSel = By.id("story-content");
        WebElement el = waitAndFind(contentSel);
        return el.getText();
    }

    public String getStoryAuthor() {
        By authorSel = By.id("story-author");
        WebElement el = waitAndFind(authorSel);
        return el.getText();
    }

    public String getMessageToast() {
        WebElement toastElem = waitAndFind(messageToast);
        return toastElem.getText();
    }

    public boolean clickMakeConnection() {
        try {
            By connectBtn = By.xpath("//button[contains(normalize-space(.), 'Maak connectie')]");
            WebElement el = waitAndFind(connectBtn);
            el.click();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getLikeCount() {
        try {
            By likeCountSel = By.xpath("//button[.//img[@alt='Like']]/following-sibling::span");
            WebElement el = waitAndFind(likeCountSel);
            String txt = el.getText();
            if (txt == null || txt.trim().isEmpty()) return 0;
            try {
                return Integer.parseInt(txt.trim());
            } catch (NumberFormatException nfe) {
                String digits = txt.replaceAll("\\D+", "");
                if (digits.isEmpty()) return 0;
                return Integer.parseInt(digits);
            }
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean clickLikeButton() {
        try {
            By likeBtn = By.xpath("//button[.//img[@alt='Like']]");
            WebElement el = waitAndFind(likeBtn);
            el.click();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean clickShareButton() {
        try {
            By shareBtn = By.xpath("//button[.//img[@alt='Deel']]");
            WebElement el = waitAndFind(shareBtn);
            el.click();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAuthModalVisible() {
        try {
            By modalHeading = By.xpath("//h2[contains(normalize-space(.), 'Je moet ingelogd zijn om dit te doen')]");
            WebElement el = new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(modalHeading));
            return el != null && el.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getStoryCondition() {
        try {
            By conditionSel = By.xpath("//span[@id='story-author']/ancestor::div[2]//div[contains(@class,'mt-2')]");
            WebElement el = waitAndFind(conditionSel);
            return el.getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns any inline error text shown on the detail page (e.g. when story not found).
     */
    public String getDetailError() {
        try {
            By errorSel = By.xpath("//article//div[contains(@class,'text-red-600')]");
            WebElement el = new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(errorSel));
            return el.getText();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isStoryNotFound() {
        String errorText = getDetailError();
        return errorText.toLowerCase().contains("verhaal niet gevonden");
    }
}
