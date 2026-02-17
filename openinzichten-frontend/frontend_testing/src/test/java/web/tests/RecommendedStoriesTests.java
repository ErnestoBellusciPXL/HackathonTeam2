package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Relevante Verhalen Functionaliteit")
public class RecommendedStoriesTests {

    private AllOpenInzichtPages pages;
    private String unique;

    private String user1, email1;
    private String user2, email2;
    private String user3, email3;

    @BeforeEach
    @Step("Setup testomgeving")
    void setup() {
        pages = new AllOpenInzichtPages();
        unique = String.valueOf(System.currentTimeMillis());

        user1 = "RecUser1" + unique;
        email1 = "rec1+" + unique + "@mail.com";

        user2 = "RecUser2" + unique;
        email2 = "rec2+" + unique + "@mail.com";

        user3 = "RecUser3" + unique;
        email3 = "rec3+" + unique + "@mail.com";
    }

    @AfterEach
    @Step("Cleanup testdata")
    void teardown() {
        deleteAccount(email1, "Password1!");
        deleteAccount(email2, "Password2!");
        deleteAccount(email3, "Password3()");
        pages.closeBrowser();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void deleteAccount(String email, String password) {
        try {
            pages.login.navigateTo();
            pages.login.login(email, password);
            sleep(800);
            pages.settings.hoverOverUsername();
            pages.settings.navigateToSettings();
            pages.settings.deleteAccount(password);
        } catch (Exception ignored) {}
    }

    private void createUser(String username, String email, String password, String condition) {
        Allure.step("Registreer gebruiker: " + username, () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(username, email, password, password);
            pages.register.registerStepTwo("3500", condition);
        });
        sleep(1500);
    }

    private void createUserWithStory(
            String username,
            String email,
            String password,
            String condition,
            String title,
            String content
    ) {
        createUser(username, email, password, condition);

        Allure.step("Schrijf verhaal: " + title, () -> {
            pages.writeStory.hoverOverUsername();
            pages.writeStory.navigateToWriteStory();
            pages.writeStory.writeStory(title, content);
        });

        sleep(1000);
        logout();
    }

    private void logout() {
        pages.settings.hoverOverUsername();
        pages.settings.navigateToSettings();
        pages.settings.clickLogout();
        sleep(500);
    }

    private void waitForRecommendedStories(int maxRetries) {
        int retry = 0;

        while (!pages.storyView.isRecommendedStoriesSectionVisible() && retry < maxRetries) {
            Allure.step("Wacht op aanbevolen verhalen (" + (retry + 1) + "/" + maxRetries + ")");
            sleep(2000);
            retry++;
        }

        assertWithMessage("Aanbevolen verhalen sectie moet zichtbaar zijn")
                .that(pages.storyView.isRecommendedStoriesSectionVisible())
                .isTrue();
    }

    private void assertWithScreenshot(Runnable assertion) {
        try {
            assertion.run();
        } catch (AssertionError e) {
            takeScreenShot();
            throw e;
        }
    }

    public void takeScreenShot(){
        Object drv = pages.getDriver();
        byte[] img = ((org.openqa.selenium.TakesScreenshot) drv).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        io.qameta.allure.Allure.addAttachment("screenshot", "image/png", new java.io.ByteArrayInputStream(img), "png");
    }

    @Test
    @Order(1)
    @Story("Verhalen met dezelfde aandoening worden aanbevolen")
    @Description("Test dat relevante verhalen worden getoond op basis van dezelfde aandoening")
    @DisplayName("Verhalen met dezelfde aandoening worden aanbevolen")
    void recommendedStoriesBasedOnSameCondition() {

        String story1 = "Depressie verhaal " + unique;

        createUserWithStory(
                user1, email1, "Password1!",
                "Depressie",
                story1,
                "Geldige inhoud, dit verhaal moet lang genoeg zijn"
        );

        createUserWithStory(
                user2, email2, "Password2!",
                "Angststoornis",
                "Angst verhaal " + unique,
                "Geldige inhoud, dit verhaal moet lang genoeg zijnt"
        );

        createUser(user3, email3, "Password3!", "Depressie");

        pages.storyView.navigateTo();
        waitForRecommendedStories(5);

        var titles = pages.storyView.getRecommendedStoryTitles();

        assertWithScreenshot(() ->
                assertWithMessage("Er moeten aanbevolen verhalen zijn")
                        .that(titles)
                        .isNotEmpty()
        );

        assertWithScreenshot(() ->
                assertWithMessage("Verhaal met dezelfde aandoening moet aanwezig zijn")
                        .that(titles)
                        .contains(story1)
        );
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.NORMAL)
    @Story("Eerste aanbevolen verhaal heeft dezelfde aandoening")
    @Description("Test dat het eerste aanbevolen verhaal van een gebruiker met dezelfde aandoening komt")
    @DisplayName("Eerste aanbevolen verhaal heeft dezelfde aandoening")
    void firstRecommendedStoryHasSameCondition() {

        String story1 = "DepressieFirst " + unique;

        createUserWithStory(
                user1, email1, "Password1!",
                "Depressie",
                story1,
                "Geldige inhoud, dit verhaal moet lang genoeg zijn"
        );

        createUserWithStory(
                user2, email2, "Password2!",
                "Angststoornis",
                "AngstSecond " + unique,
                "Geldige inhoud, dit verhaal moet lang genoeg zijn"
        );

        createUser(user3, email3, "Password3!", "Depressie");

        pages.storyView.navigateTo();
        waitForRecommendedStories(3);

        String firstTitle = pages.storyView.getFirstRecommendedStoryTitle();

        assertWithScreenshot(() ->
                assertWithMessage("Eerste aanbevolen verhaal moet hetzelfde aandoeningstype hebben")
                        .that(firstTitle)
                        .isEqualTo(story1)
        );
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.CRITICAL)
    @Story("Verhaal met dezelfde aandoening krijgt prioriteit")
    @Description("Test dat voor een gebruiker het verhaal met dezelfde aandoening als eerste wordt getoond")
    @DisplayName("Verhaal met dezelfde aandoening krijgt prioriteit")
    void sameConditionStoryIsPrioritized() {

        String story1 = "PriorityStory " + unique;

        createUserWithStory(
                user1, email1, "Password1!",
                "Depressie",
                story1,
                "Geldige inhoud, dit verhaal moet lang genoeg zijn"
        );

        createUserWithStory(
                user2, email2, "Password2!",
                "Angststoornis",
                "OtherStory " + unique,
                "Geldige inhoud, dit verhaal moet lang genoeg zijn"
        );

        createUser(user3, email3, "Password3!", "Depressie");

        pages.storyView.navigateTo();
        waitForRecommendedStories(3);

        String first = pages.storyView.getFirstRecommendedStoryTitle();

        assertWithScreenshot(() ->
                assertWithMessage(
                        "Voor gebruiker met Depressie moet verhaal met Depressie eerst komen"
                )
                        .that(first)
                        .isEqualTo(story1)
        );
    }
}