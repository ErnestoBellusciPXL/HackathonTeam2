package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;


import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Filtering op Aandoening")
public class ConditionFilterTests {

    private AllOpenInzichtPages pages;
    private String user1Email;
    private String user2Email;
    private String user3Email;
    private String password1 = "Password1!";
    private String password2 = "Password2!";

    @BeforeEach
    @Description("Initialiseer de pagina-objecten voor de tests")
    public void setup() {
        pages = new AllOpenInzichtPages();
    }

    @AfterEach
    @Description("Verwijder alle aangemaakte accounts na de tests")
    public void teardown() {
        if (pages != null) {
            // Try to delete all accounts that were created during the test
            if (user1Email != null) {
                try {
                    pages.login.navigateTo();
                    pages.login.loginAndWait(user1Email, password1, java.time.Duration.ofSeconds(6));
                    if (pages.settings.isUserLoggedIn()) {
                        pages.settings.hoverOverUsername();
                        pages.settings.navigateToSettings();
                        pages.settings.deleteAccountAndWait(password1, "Je account is succesvol verwijderd");
                    }
                } catch (Exception ignored) {}
            }

            if (user2Email != null) {
                try {
                    pages.login.navigateTo();
                    pages.login.loginAndWait(user2Email, password2, java.time.Duration.ofSeconds(6));
                    if (pages.settings.isUserLoggedIn()) {
                        pages.settings.hoverOverUsername();
                        pages.settings.navigateToSettings();
                        pages.settings.deleteAccountAndWait(password2, "Je account is succesvol verwijderd");
                    }
                } catch (Exception ignored) {}
            }

            if (user3Email != null) {
                try {
                    pages.login.navigateTo();
                    pages.login.loginAndWait(user3Email, password1, java.time.Duration.ofSeconds(6));
                    if (pages.settings.isUserLoggedIn()) {
                        pages.settings.hoverOverUsername();
                        pages.settings.navigateToSettings();
                        pages.settings.deleteAccountAndWait(password1, "Je account is succesvol verwijderd");
                    }
                } catch (Exception ignored) {}
            }

            pages.closeBrowser();
        }
    }

    @Test
    @Order(1)
    @Story("Filter verhalen op aandoening")
    @Description("Controleert dat wanneer op een aandoening wordt gefilterd, alleen verhalen met die aandoening worden weergegeven.")
    @DisplayName("Testen van filtering van verhalen op aandoening")
    public void filterByConditionShowsOnlyRelevantStory() throws InterruptedException {
        String unique = String.valueOf(System.currentTimeMillis());
        user1Email = "user1+" + unique + "@mail.com";
        user2Email = "user2+" + unique + "@mail.com";
        
        try {
            // ===== Account 1: Create story with Depressie =====
            Allure.step("Registreer eerste gebruiker met aandoening Depressie", () -> {
                pages.register.navigateTo();
                pages.register.registerStepOne("User1" + unique, user1Email, password1, password1);
                pages.register.registerStepTwo("3500", "Depressie");
                Thread.sleep(1200);
            });

            Allure.step("Schrijf verhaal met Depressie", () -> {
                pages.writeStory.navigateTo();
                String story1Title = "Depressie Verhaal " + unique;
                String story1Content = "Dit is een verhaal over depressie. Dit is de inhoud van het testverhaal die lang genoeg moet zijn.";
                pages.writeStory.writeStory(story1Title, story1Content);

                assertWithScreenshotOnFailure(() ->
                        assertWithMessage("Verhaal 1 moet succesvol gepubliceerd zijn")
                                .that(pages.writeStory.isOnHomePage())
                                .isTrue()
                );
            });

            Allure.step("Log uit", () -> {
                pages.settings.hoverOverUsername();
                pages.settings.navigateToSettings();
                pages.settings.clickLogout();
                Thread.sleep(800);
            });

            // ===== Account 2: Create story with Diabetes =====
            Allure.step("Registreer tweede gebruiker met aandoening Diabetes", () -> {
                pages.register.navigateTo();
                pages.register.registerStepOne("User2" + unique, user2Email, password2, password2);
                pages.register.registerStepTwo("3500", "Diabetes");
                Thread.sleep(1200);
            });

            Allure.step("Schrijf verhaal met Diabetes", () -> {
                pages.writeStory.navigateTo();
                String story2Title = "Diabetes Verhaal " + unique;
                String story2Content = "Dit is een verhaal over diabetes. Dit is de inhoud van het testverhaal die lang genoeg moet zijn.";
                pages.writeStory.writeStory(story2Title, story2Content);

                assertWithScreenshotOnFailure(() ->
                        assertWithMessage("Verhaal 2 moet succesvol gepubliceerd zijn")
                                .that(pages.writeStory.isOnHomePage())
                                .isTrue()
                );
            });

            Allure.step("Log uit", () -> {
                pages.settings.hoverOverUsername();
                pages.settings.navigateToSettings();
                pages.settings.clickLogout();
                Thread.sleep(800);
            });

            // ===== Filter by Depressie and verify only 1 story is shown =====
            Allure.step("Navigeer naar verhalenpagina", () -> {
                pages.storyView.navigateTo();
                Thread.sleep(800);
            });

            Allure.step("Controleer aantal verhalen voordat gefilterd wordt", () -> {
                int countBeforeFilter = pages.storyView.getStoriesCount();

                assertWithScreenshotOnFailure(() ->
                        assertWithMessage("Er moeten minimaal 2 verhalen zichtbaar zijn voordat we filteren")
                                .that(countBeforeFilter)
                                .isAtLeast(2)
                );
            });

            Allure.step("Filter op aandoening Depressie", () -> {
                pages.storyView.filterByCondition("Depressie");
                Thread.sleep(1500); // Wait for filter to apply
            });

            Allure.step("Valideer dat slechts 1 verhaal wordt weergegeven", () -> {
                int countAfterFilter = pages.storyView.getStoriesCount();

                assertWithScreenshotOnFailure(() ->
                        assertWithMessage("Na filteren op Depressie moet exact 1 verhaal zichtbaar zijn")
                                .that(countAfterFilter)
                                .isEqualTo(1)
                );
            });

            Allure.step("Controleer dat het juiste verhaal wordt getoond", () -> {
                String shownTitle = pages.storyView.getFirstStoryPreviewTitle();

                assertWithScreenshotOnFailure(() ->
                        assertWithMessage("Het getoonde verhaal moet over Depressie gaan")
                                .that(shownTitle)
                                .contains("Depressie")
                );
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(2)
    @Story("Filter op aandoening zonder verhalen")
    @Description("Controleert dat wanneer op een aandoening wordt gefilterd waar geen verhalen voor bestaan, geen verhalen worden weergegeven.")
    @DisplayName("Testen van filtering op aandoening zonder verhalen")
    public void filterByConditionWithNoStoriesShowsEmpty() throws InterruptedException {
        String unique = String.valueOf(System.currentTimeMillis());
        user3Email = "user3+" + unique + "@mail.com";
        
        try {
            // ===== Account 1: Create story with Depressie =====
            Allure.step("Registreer gebruiker met aandoening Depressie", () -> {
                pages.register.navigateTo();
                pages.register.registerStepOne("User3" + unique, "user3+" + unique + "@mail.com", "Password1!", "Password1!");
                pages.register.registerStepTwo("3500", "Depressie");
                Thread.sleep(1200);
            });

            Allure.step("Schrijf verhaal met Depressie", () -> {
                pages.writeStory.navigateTo();
                String story1Title = "Depressie Verhaal " + unique;
                String story1Content = "Dit is een verhaal over depressie. Dit is de inhoud van het testverhaal die lang genoeg moet zijn.";
                pages.writeStory.writeStory(story1Title, story1Content);

                assertWithScreenshotOnFailure(() ->
                        assertWithMessage("Verhaal moet succesvol gepubliceerd zijn")
                                .that(pages.writeStory.isOnHomePage())
                                .isTrue()
                );
            });

            Allure.step("Log uit", () -> {
                pages.settings.hoverOverUsername();
                pages.settings.navigateToSettings();
                pages.settings.clickLogout();
                Thread.sleep(800);
            });

            // ===== Navigate to story view and verify initial count =====
            Allure.step("Navigeer naar verhalenpagina", () -> {
                pages.storyView.navigateTo();
                Thread.sleep(800);
            });

            Allure.step("Controleer dat er verhalen beschikbaar zijn", () -> {
                int countBeforeFilter = pages.storyView.getStoriesCount();

                assertWithScreenshotOnFailure(() ->
                        assertWithMessage("Er moeten minimaal 1 verhaal zichtbaar zijn voordat we filteren")
                                .that(countBeforeFilter)
                                .isAtLeast(1)
                );
            });

            // ===== Filter by Diabetes (which should have no stories) and verify empty result =====
            Allure.step("Filter op aandoening Diabetes (waar geen verhalen voor bestaan)", () -> {
                pages.storyView.filterByCondition("Diabetes");
                Thread.sleep(1500); // Wait for filter to apply
            });

            Allure.step("Valideer dat geen verhalen worden weergegeven", () -> {
                int countAfterFilter = pages.storyView.getStoriesCount();

                assertWithScreenshotOnFailure(() ->
                        assertWithMessage("Na filteren op Diabetes (waar geen verhalen voor zijn) moeten 0 verhalen zichtbaar zijn.")
                                .that(countAfterFilter)
                                .isEqualTo(0)
                );
            });

        } finally {
            try {
                pages.settings.navigateToSettings();
                pages.settings.deleteAccount("Password1!");
            } catch (Exception ignored) {}
            pages.closeBrowser();
        }
    }


    public void takeScreenShot(){
        Object drv = pages.getDriver();
        byte[] img = ((org.openqa.selenium.TakesScreenshot) drv).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        io.qameta.allure.Allure.addAttachment("screenshot", "image/png", new java.io.ByteArrayInputStream(img), "png");
    }

    private void assertWithScreenshotOnFailure(Runnable assertion) {
        try {
            assertion.run();
        } catch (AssertionError e) {
            takeScreenShot();
            throw e;
        }
    }
}
