package web.pageObjects;

import org.openqa.selenium.WebDriver;
import web.browser.BrowserUtil;

public class AllOpenInzichtPages {
    public LoginPage login;
    public RegisterPage register;
    public WriteStoryPage writeStory;
    public SecureAreaPage secureArea;
    public SettingsPage settings;
    public ConnectionsPage connections;
    public StoryViewPage storyView;
    public StoryDetailPage storyDetail;
    public ChatLauncherPage chatLauncher;
    public AdminPage admin;
    public AdminUserDetailPage adminUserDetail;
    public MyStoryPage myStory;
    public TermsPage terms;
    public HomePage home;
    public PrivacyPolicyPage privacyPolicy;
    public CodeOfConductPage codeOfConductPage;
    public AboutPage about;
    public ErrorPage error;
    private WebDriver driver;
    public WebDriver getDriver() {
        return driver;
    }

    public AllOpenInzichtPages(){
        driver = BrowserUtil.createBrowser();
        this.login = new LoginPage(driver);
        this.register = new RegisterPage(driver);
        this.writeStory = new WriteStoryPage(driver);
        this.secureArea = new SecureAreaPage(driver);
        this.settings = new SettingsPage(driver);
        this.connections = new ConnectionsPage(driver);
        this.storyView = new StoryViewPage(driver);
        this.storyDetail = new StoryDetailPage(driver);
        this.chatLauncher = new ChatLauncherPage(driver);
        this.admin = new AdminPage(driver);
        this.adminUserDetail = new AdminUserDetailPage(driver);
        this.myStory = new MyStoryPage(driver);
        this.terms = new TermsPage(driver);
        this.home = new HomePage(driver);
        this.codeOfConductPage = new CodeOfConductPage(driver);
        this.privacyPolicy = new PrivacyPolicyPage(driver);
        this.about = new AboutPage(driver);
        this.error = new ErrorPage(driver);
    }

    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }
}