package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Verhaal vind-ik-leuks en delen")
public class StoryLikeTests {

    private AllOpenInzichtPages pages;
    private final List<Account> createdAccounts = new ArrayList<>();

    record Account(String email, String password) {}

    @BeforeEach
    @Step("Setup: browser initialiseren")
    public void setup() {
        pages = new AllOpenInzichtPages();
    }

    @AfterEach
    @Step("Teardown: alle testaccounts verwijderen")
    void tearDown() {
        for (Account acc : createdAccounts) {
            try {
                Allure.step("Account verwijderen: " + acc.email(), () -> {
                    pages.login.navigateTo();
                    pages.login.login(acc.email(), acc.password());
                    Thread.sleep(800);
                    pages.settings.hoverOverUsername();
                    pages.settings.navigateToSettings();
                    pages.settings.deleteAccount(acc.password());
                });
            } catch (Exception ignored) {}
        }
        createdAccounts.clear();

        if (pages != null) pages.closeBrowser();
    }

    @Test
    @Order(1)
    @Story("Like teller")
    @Description("Controleert dat de like teller correct verhoogt wanneer een andere gebruiker een verhaal liket.")
    @DisplayName("Like teller verhoogt wanneer andere gebruiker liket")
    public void likeDisplayUpdates_whenAnotherUserLikes() throws InterruptedException {
        String unique = unique();

        String emailA = "storyA+" + unique + "@mail.com";
        String passA = "Password1!";
        String emailB = "storyB+" + unique + "@mail.com";
        String passB = "Password2!";

        createdAccounts.add(new Account(emailA, passA));
        createdAccounts.add(new Account(emailB, passB));

        String title = "LikeTestStory";
        String content = "Inhoud voor like test. Lang genoeg om geldig te zijn.";

        Allure.step("Gebruiker A publiceert een verhaal", () ->
                registerAndWriteStory("StoryUserA" + unique, emailA, passA, title, content)
        );

        Allure.step("Gebruiker A logt uit", this::logout);

        Allure.step("Gebruiker B liket het verhaal", () -> {
            registerUser("StoryUserB" + unique, emailB, passB);
            pages.storyView.openStoryByTitle(title);
            Thread.sleep(800);

            int before = pages.storyDetail.getLikeCount();

            Allure.step("Controleer Like count", () -> {
                assertWithScreenshot(() ->
                        assertWithMessage("Like count moet leesbaar zijn")
                                .that(before)
                                .isAtLeast(0)
                );
            });

            Allure.step("Controleer Like knop klik", () -> {
                assertWithScreenshot(() ->
                        assertWithMessage("Like knop kon niet aangeklikt worden")
                                .that(pages.storyDetail.clickLikeButton())
                                .isTrue()
                );
            });

            Thread.sleep(800);

            int after = pages.storyDetail.getLikeCount();

            Allure.step("Controleer Like count na klikken", () -> {
                assertWithScreenshot(() ->
                        assertWithMessage("Like count verhoogt niet met 1")
                                .that(after)
                                .isEqualTo(before + 1)
                );
            });
        });
    }

    @Test
    @Order(2)
    @Story("Delen")
    @Description("Controleert dat bij het delen van een verhaal de link wordt gekopieerd en een succesmelding wordt getoond.")
    @DisplayName("Delen kopieert link en toont succesmelding")
    public void shareCopiesLink_showsSuccessToast() throws InterruptedException {
        String unique = unique();
        String email = "share+" + unique + "@mail.com";
        String pass = "Password1!";

        createdAccounts.add(new Account(email, pass));

        String title = "ShareTestStory";
        String content = "Dit is een geldige verhaal voor deze test. Lang genoeg om te voldoen.";

        Allure.step("Gebruiker publiceert een verhaal", () ->
                registerAndWriteStory("ShareUser" + unique, email, pass, title, content)
        );

        Allure.step("Gebruiker deelt het verhaal", () -> {
            pages.storyView.openStoryByTitle(title);
            Thread.sleep(800);

            Allure.step("Controleer Share knop klik", () -> {
                assertWithScreenshot(() ->
                        assertWithMessage("Share knop kon niet aangeklikt worden")
                                .that(pages.storyDetail.clickShareButton())
                                .isTrue()
                );
            });

            Thread.sleep(800);
        });

        Allure.step("Then succes toast wordt getoond", () -> {
            String toast = pages.storyDetail.getMessageToast();

            Allure.step("Controleer succes toast", () -> {
                assertWithScreenshot(() ->
                        assertWithMessage("Geen 'gekopieerd' melding zichtbaar")
                                .that(toast)
                                .contains("gekopieerd")
                );
            });
        });
    }

    @Test
    @Order(2)
    @Story("Like restrictie")
    @Description("Controleert dat een niet-ingelogde gebruiker een authenticatie modal te zien krijgt bij het proberen te liken van een verhaal.")
    @DisplayName("Niet-ingelogde gebruiker ziet auth modal bij liken")
    public void likeRestriction_notLoggedIn_showsAuthModal() throws InterruptedException {
        String unique = unique();
        String email = "anon+" + unique + "@mail.com";
        String pass = "Password1!";

        createdAccounts.add(new Account(email, pass));

        String title = "AnonLikeTest";
        String content = "Dit is een geldige verhaal voor deze test. Lang genoeg om te voldoen.";

        Allure.step("Given een verhaal bestaat", () ->
                registerAndWriteStory("AnonUser" + unique, email, pass, title, content)
        );

        Allure.step("And gebruiker logt uit", this::logout);

        Allure.step("When anonieme gebruiker probeert te liken", () -> {
            pages.storyView.openStoryByTitle(title);
            Thread.sleep(800);
            pages.storyDetail.clickLikeButton();
            Thread.sleep(500);
        });

        Allure.step("Then Login / Registreer pop-up wordt getoond", () ->

                assertWithScreenshot(() ->
                        assertWithMessage("Login / Registreer pop-up wordt niet getoond")
                                .that(pages.storyDetail.isAuthModalVisible())
                                .isTrue())
        );
    }

    /* helpers */

    @Step("Registreer gebruiker en publiceer verhaal")
    private void registerAndWriteStory(
            String username,
            String email,
            String password,
            String title,
            String content
    ) throws InterruptedException {

        registerUser(username, email, password);

        pages.writeStory.hoverOverUsername();
        pages.writeStory.navigateToWriteStory();
        pages.writeStory.writeStory(title, content);
        Thread.sleep(800);

        assertWithMessage("Verhaal werd niet gepubliceerd")
                .that(pages.writeStory.isOnHomePage())
                .isTrue();
    }

    @Step("Registreer gebruiker")
    private void registerUser(String username, String email, String password) throws InterruptedException {
        pages.register.navigateTo();
        pages.register.registerStepOne(username, email, password, password);
        pages.register.registerStepTwo("3500", "Depressie");
        Thread.sleep(1200);
    }

    @Step("Gebruiker logt uit")
    private void logout() {
        pages.settings.hoverOverUsername();
        pages.settings.navigateToSettings();
        pages.settings.clickLogout();
    }

    private String unique() {
        return String.valueOf(System.currentTimeMillis()).substring(6);
    }

    private void assertWithScreenshot(Runnable assertion) {
        try {
            assertion.run();
        } catch (AssertionError e) {
            takeScreenshot();
            throw e;
        }
    }

    private void takeScreenshot() {
        byte[] img = ((org.openqa.selenium.TakesScreenshot) pages.getDriver())
                .getScreenshotAs(org.openqa.selenium.OutputType.BYTES);

        Allure.addAttachment(
                "Screenshot bij falende assert",
                "image/png",
                new java.io.ByteArrayInputStream(img),
                "png"
        );
    }
}
