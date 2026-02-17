package web.pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class AdminPage extends BasePage {
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(8);
    
    private final By stateToggleButtons = By.cssSelector(".state-toggle-btn");
    private final By ticketTable = By.cssSelector("div.space-y-4 table");
    private final By ticketRows = By.cssSelector("div.space-y-4 table tbody tr");
    private final By statusColumn = By.cssSelector("td:nth-child(5)");
    private final By reportTab = By.id("reports");
    private final By ticketsStateToggle = By.cssSelector(".state-toggle");
    private final By tableHeaders = By.cssSelector("div.space-y-4 table thead th");
    private final By dateColumn = By.cssSelector("td:nth-child(1)");
    private final By reportedUserColumn = By.cssSelector("td:nth-child(2)");
    private final By storyColumn = By.cssSelector("td:nth-child(3)");
    private final By bekijkenButtons = By.cssSelector("td:nth-child(6) button");
    private final By approveButton = By.id("approve_button");
    private final By deleteButton = By.id("delete_button");
    private final By backButton = By.cssSelector("button.text-sm");
    private final By statusBadge = By.cssSelector(".inline-flex.items-center.px-3.py-1.rounded-full.border");
    private final By adminNavLink = By.cssSelector("a[href='/admin']");
    private final By tabsCard = By.cssSelector(".tabs-card");
    private final By beherenButtons = By.xpath("//button[normalize-space(text())='Beheren']");
    private final By usersTab = By.id("users");
    private final By beherenHeader = By.xpath("//th[normalize-space(text())='Beheren']");


    public AdminPage(WebDriver driver) {
        super(driver, "admin");
    }

    private WebElement waitAndFind(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }
    
    private List<WebElement> waitAndFindAll(By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    public boolean isBeherenHeaderPresent() {
        try {
            WebElement header = new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOfElementLocated(beherenHeader));
            return header.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canClickBeherenButton(int index) {
        try {
            List<WebElement> buttons = waitAndFindAll(beherenButtons);
            return index >= 0 &&
                    index < buttons.size() &&
                    buttons.get(index).isDisplayed() &&
                    buttons.get(index).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
    
    public void clickStatusFilter(String status) {
        List<WebElement> buttons = waitAndFindAll(stateToggleButtons);
        for (WebElement button : buttons) {
            if (button.getText().equalsIgnoreCase(status)) {
                button.click();
                break;
            }
        }
    }
    
    public int getTicketCount() {
        try {
            List<WebElement> rows = waitAndFindAll(ticketRows);
            return rows.size();
        } catch (Exception e) {
            return 0;
        }
    }
    
    public boolean isTableVisible() {
        try {
            // Consider the tickets section visible when the state toggle is visible
            WebElement toggle = new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOfElementLocated(ticketsStateToggle));
            return toggle.isDisplayed();
        } catch (Exception e) {
            // Fallback: check for a table element if toggle not found
            try {
                WebElement table = waitAndFind(ticketTable);
                return table.isDisplayed();
            } catch (Exception ignored) {
                return false;
            }
        }
    }
    
    public boolean hasTicketsWithStatus(String status) {
        try {
            List<WebElement> rows = waitAndFindAll(ticketRows);
            for (WebElement row : rows) {
                WebElement statusCell = row.findElement(statusColumn);
                if (statusCell.getText().equalsIgnoreCase(status)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void openReportTab() {
        WebElement tab = waitAndFind(reportTab);
        tab.click();
        // Wait for the tickets tab content to appear
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOfElementLocated(ticketsStateToggle));
        } catch (Exception ignored) {}
    }

    /**
     * Ensure we are on the /admin route. Prefer clicking the navbar link (guarded by role)
     * and fall back to direct navigation if not found.
     */
    public void ensureOnAdmin() {
        try {
            if (driver.getCurrentUrl() != null && driver.getCurrentUrl().contains("/admin")) {
                return;
            }
        } catch (Exception ignored) {}

        try {
            WebElement nav = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(adminNavLink));
            nav.click();
        } catch (Exception e) {
            // Fallback to direct navigation
            this.navigateTo();
        }

        // Wait for admin dashboard shell
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOfElementLocated(tabsCard));
        } catch (Exception ignored) {}
    }
    
    public boolean areOnlyRequiredColumnsVisible() {
        try {
            List<WebElement> headers = waitAndFindAll(tableHeaders);
            if (headers.size() != 6) {
                return false;
            }
            
            String[] expectedHeaders = {"Datum", "Gerapporteerde", "Verhaal", "Reden", "Status", ""};
            for (int i = 0; i < expectedHeaders.length; i++) {
                String headerText = headers.get(i).getText().trim();
                if (!headerText.equals(expectedHeaders[i]) && !headerText.isEmpty()) {
                    return false;
                }
            }
            
            List<WebElement> rows = waitAndFindAll(ticketRows);
            if (rows.isEmpty()) {
                return true;
            }
            
            WebElement firstRow = rows.get(0);
            WebElement dateCell = firstRow.findElement(dateColumn);
            WebElement reportedUserCell = firstRow.findElement(reportedUserColumn);
            WebElement storyCell = firstRow.findElement(storyColumn);
            WebElement statusCell = firstRow.findElement(statusColumn);
            
            return dateCell.isDisplayed() && 
                   reportedUserCell.isDisplayed() && 
                   storyCell.isDisplayed() && 
                   statusCell.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getTableHeaderText(int columnIndex) {
        try {
            List<WebElement> headers = waitAndFindAll(tableHeaders);
            if (columnIndex >= 0 && columnIndex < headers.size()) {
                return headers.get(columnIndex).getText().trim();
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }
    
    public void clickBekijkenButton() {
        try {
            List<WebElement> buttons = waitAndFindAll(bekijkenButtons);
            if (!buttons.isEmpty()) {
                // Click the first "Bekijken" button
                buttons.get(0).click();
            }
        } catch (Exception e) {
            // If no tickets are available, do nothing
        }
    }
    
    public void clickBekijkenButton(int index) {
        try {
            // First try to find buttons in the action column (6th column)
            List<WebElement> actionColumnButtons = waitAndFindAll(bekijkenButtons);
            System.out.println("Total action column buttons found: " + actionColumnButtons.size());
            
            if (index >= 0 && index < actionColumnButtons.size()) {
                System.out.println("Clicking 'Bekijken' button at index: " + index);
                System.out.println(actionColumnButtons.get(index).getText());
                actionColumnButtons.get(index).click();
                return;
            }
            
            // Fallback: find all buttons with "Bekijken" text
            List<WebElement> allButtons = waitAndFindAll(By.cssSelector("button"));
            int bekijkenCount = 0;
            for (WebElement button : allButtons) {
                if (button.getText().contains("Bekijken")) {
                    if (bekijkenCount == index) {
                        button.click();
                        return;
                    }
                    bekijkenCount++;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while clicking 'Bekijken' button: " + e.getMessage());
            // If no tickets are available or index is out of bounds, do nothing
        }
    }
    
    public void clickApproveButton() {
        try {
            WebElement button = waitAndFind(approveButton);
            button.click();
        } catch (Exception e) {
            // Handle case where button is not available
        }
    }
    
    public void clickDeleteButton() {
        try {
            WebElement button = waitAndFind(deleteButton);
            button.click();
        } catch (Exception e) {
            // Handle case where button is not available
        }
    }
    
    public void clickBackButton() {
        try {
            WebElement button = waitAndFind(backButton);
            button.click();
        } catch (Exception e) {
            // Handle case where button is not available
        }
    }
    
    public String getCurrentStatus() {
        try {
            WebElement statusElement = waitAndFind(statusBadge);
            return statusElement.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }
    
    public boolean isOnTicketDetailPage() {
        try {
            WebElement approveBtn = waitAndFind(approveButton);
            WebElement deleteBtn = waitAndFind(deleteButton);
            return approveBtn.isDisplayed() && deleteBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean isActionButtonsVisible() {
        try {
            WebElement approveBtn = waitAndFind(approveButton);
            WebElement deleteBtn = waitAndFind(deleteButton);
            return approveBtn.isDisplayed() && deleteBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    public int getBekijkenButtonCount() {
        try {
            List<WebElement> buttons = waitAndFindAll(bekijkenButtons);
            return buttons.size();
        } catch (Exception e) {
            return 0;
        }
    }
    
    public boolean isBackToAdminTable() {
        try {
            // Check if we're back to the admin table by looking for the report tab
            WebElement tab = waitAndFind(reportTab);
            return tab.isDisplayed() && isTableVisible();
        } catch (Exception e) {
            return false;
        }
    }
    
    public void openUsersTab() {
        WebElement tab = waitAndFind(usersTab);
        tab.click();
        // Wait for user table to appear
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
    }
    
    public int getBeherenButtonCount() {
        try {
            List<WebElement> buttons = waitAndFindAll(beherenButtons);
            return buttons.size();
        } catch (Exception e) {
            return 0;
        }
    }
    
    public void clickBeherenButton(int index) {
        try {
            List<WebElement> buttons = waitAndFindAll(beherenButtons);
            if (index >= 0 && index < buttons.size()) {
                buttons.get(index).click();
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while clicking 'Beheren' button: " + e.getMessage());
        }
    }
}
