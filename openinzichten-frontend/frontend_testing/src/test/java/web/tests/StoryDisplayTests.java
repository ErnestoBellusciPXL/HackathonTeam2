package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Weergave van verhalen")
public class StoryDisplayTests {
    private AllOpenInzichtPages pages;
    private final String password1 = "Password1!";
    private final java.util.List<String> createdEmails = new java.util.ArrayList<>();


    @BeforeEach
    @Step("Setup: initialiseer browserpagina's")
    public void setup() {
        pages = new AllOpenInzichtPages();
    }

    @AfterEach
    @Step("Teardown: verwijder testgebruikers en sluit browser")
    public void tearDown() {
        for (String email : createdEmails) {
            try {
                Allure.step("Verwijder account: " + email, () -> {
                    pages.login.navigateTo();
                    pages.login.login(email, password1);
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}

                    pages.settings.hoverOverUsername();
                    pages.settings.navigateToSettings();
                    pages.settings.deleteAccount(password1);
                });
            } catch (Exception ignored) {
                // bewust genegeerd: teardown mag tests niet breken
            }
        }

        createdEmails.clear();

        if (pages != null) {
            pages.closeBrowser();
        }
    }

    @Test
    @Order(1)
    @Story("Ongeldig verhaal ID")
    @Description("Controleert dat bij navigeren naar een niet-bestaand verhaal ID een foutmelding wordt getoond.")
    @DisplayName("Ongeldig verhaal ID toont foutmelding")
    public void invalidStoryIdShowsError() throws InterruptedException {
        String badId = "nonexistent-" + System.currentTimeMillis();

        Allure.step("When gebruiker navigeert naar een niet-bestaand verhaal", () -> {
            pages.getDriver()
                    .navigate()
                    .to(pages.storyDetail.baseurl + "story/" + badId);
        });

        Allure.step("Then inline foutmelding wordt getoond", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("Geen foutmelding zichtbaar bij ongeldig verhaal-ID")
                            .that(pages.storyDetail.getDetailError())
                            .isNotEmpty()
            );
        });
    }

    @Test
    @Order(2)
    @Story("Verhaal detailweergave")
    @Description("Controleert dat de detailpagina van een verhaal de juiste titel, auteur, aandoening en inhoud toont.")
    @DisplayName("Verhaal detailweergave toont correcte informatie")
    public void detailShowsTitleAuthorConditionAndSnippet() throws InterruptedException {
        String unique = unique();
        String title = "TestTitle" + unique;
        String content = "Dit is de inhoud van het testverhaal. Lang genoeg voor preview.";

        Allure.step("Given gebruiker A registreert en publiceert een verhaal", () -> {
            registerUser("StoryUser" + unique, "story+" + unique + "@mail.com", password1);
            pages.writeStory.navigateTo();
            Thread.sleep(800);
            pages.writeStory.writeStory(title, content);

            assertWithMessage("Publiceren van verhaal mislukt")
                    .that(pages.writeStory.isOnHomePage())
                    .isTrue();

            logout();
        });

        Allure.step("And gebruiker B registreert zich", () -> {
            registerUser("StoryUser" + unique + "2", "story2+" + unique + "@mail.com", password1);
        });

        Allure.step("When gebruiker B het verhaal opent", () -> {
            pages.storyView.openStoryByTitle(title);
        });

        Allure.step("Then alle details worden correct weergegeven", () -> {
            assertWithScreenshot(() -> {
                assertWithMessage("Titel klopt niet")
                        .that(pages.storyDetail.getStoryTitle())
                        .isEqualTo(title);

                assertWithMessage("Auteur klopt niet")
                        .that(pages.storyDetail.getStoryAuthor())
                        .isEqualTo("StoryUser" + unique);

                assertWithMessage("Aandoening klopt niet")
                        .that(pages.storyDetail.getStoryCondition())
                        .isEqualTo("Depressie");

                assertWithMessage("Inhoud ontbreekt of is te kort")
                        .that(pages.storyDetail.getStoryContent().length())
                        .isGreaterThan(20);
            });
        });
    }

    @Test
    @Order(3)
    @Story("Verhalenoverzicht weergave")
    @Description("Controleert dat het verhalenoverzicht minstens één verhaal toont met een titel.")
    @DisplayName("Verhalenoverzicht toont lijst met verhalen")
    public void storiesOverviewDisplaysList() throws InterruptedException {
        String unique = unique();

        Allure.step("Given gebruiker publiceert een verhaal", () -> {
            registerUser("StoryUser" + unique, "story+" + unique + "@mail.com", password1);
            pages.writeStory.navigateTo();
            Thread.sleep(800);
            pages.writeStory.writeStory(
                    "OverviewTitle" + unique,
                    "Preview content for overview test."
            );

            assertWithMessage("Verhaal werd niet gepubliceerd")
                    .that(pages.writeStory.isOnHomePage())
                    .isTrue();
        });

        Allure.step("When gebruiker naar verhalenoverzicht navigeert", () -> {
            pages.storyView.navigateTo();
        });

        Allure.step("Then overzicht bevat minstens één verhaal met titel", () -> {
            assertWithScreenshot(() -> {
                assertWithMessage("Geen verhalen zichtbaar in overzicht")
                        .that(pages.storyView.getStoriesCount())
                        .isGreaterThan(0);

                assertWithMessage("Eerste verhaal heeft geen titel")
                        .that(pages.storyView.getFirstStoryPreviewTitle())
                        .isNotEmpty();
            });
        });
    }

    private void registerUser(String username, String email, String password) throws InterruptedException {
        pages.register.navigateTo();
        pages.register.registerStepOne(username, email, password, password);
        pages.register.registerStepTwo("3500", "Depressie");
        Thread.sleep(1200);

        assertWithMessage("Gebruiker niet ingelogd na registratie")
                .that(pages.settings.isUserLoggedIn())
                .isTrue();

        createdEmails.add(email);
    }

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
