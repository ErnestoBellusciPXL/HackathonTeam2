package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Verhaal schrijven")
public class WriteStoryTests {

    private AllOpenInzichtPages pages;
    private String unique;
    private String storyTitle;
    private String storyContent;
    private final String password = "Password1!";

    @BeforeEach
    @Step("Setup: registreer gebruiker en navigeer naar schrijfpagina")
    void setup() {
        pages = new AllOpenInzichtPages();
        unique = String.valueOf(System.currentTimeMillis());
        storyTitle = "Mijn Eerste Verhaal";
        storyContent = "Dit is de inhoud van mijn verhaal. Deze moet wel lang genoeg zijn natuurlijk";

        Allure.step("Registreer gebruiker", () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(
                    "StoryUser" + unique,
                    "story+" + unique + "@mail.com",
                    password,
                    password
            );
            pages.register.registerStepTwo("3500", "Depressie");
            Thread.sleep(1200);
        });

        Allure.step("Navigeer naar schrijfpagina", () -> {
            pages.writeStory.hoverOverUsername();
            pages.writeStory.navigateToWriteStory();
        });
    }

    @Test
    @Order(1)
    @Story("Succesvol verhaal posten")
    @Description("Controleert dat een geldig verhaal succesvol gepost kan worden en de gebruiker naar de homepage wordt gerouteerd.")
    @DisplayName("Succesvol verhaal posten met geldige titel en inhoud")
    void submit_valid_story() {
        Allure.step("Post geldig verhaal", () -> {
            pages.writeStory.writeStory(storyTitle, storyContent);
        });

        Allure.step("Controleer redirect naar homepage", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Na succes moet naar homepage geroute worden")
                                .that(pages.writeStory.isOnHomePage())
                                .isTrue()
                )
        );
    }

    @Test
    @Order(2)
    @Story("Verhaal posten met titel leeg")
    @Description("Controleert dat verhalen zonder titel niet gepost kunnen worden en de juiste foutmeldingen tonen.")
    @DisplayName("Verhaal posten met lege titel toont fout")
    void submit_empty_title_shows_error() {
        Allure.step("Post verhaal met lege titel", () -> {
            pages.writeStory.writeStory("", storyContent);
        });

        Allure.step("Controleer foutmelding voor lege titel", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Verwachte foutmelding voor lege titel")
                                .that(pages.writeStory.getMessageToast())
                                .contains("Vul een titel in.")
                )
        );
    }

    @Test
    @Order(3)
    @Story("Verhaal posten met te korte titel")
    @Description("Controleert dat verhalen met te korte titels niet gepost kunnen worden en de juiste foutmeldingen tonen.")
    @DisplayName("Verhaal posten met te korte titel toont fout")
    void submit_title_below_minimum_shows_error() {
        Allure.step("Post verhaal met te korte titel", () -> {
            pages.writeStory.writeStory("abcd", storyContent);
        });

        Allure.step("Controleer foutmelding voor te korte titel", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Verwachte foutmelding voor te korte titel")
                                .that(pages.writeStory.getMessageToast())
                                .contains("Titel moet minimaal 5 tekens bevatten.")
                )
        );
    }

    @Test
    @Order(4)
    @Story("Verhaal posten met te lange titel")
    @Description("Controleert dat verhalen met te lange titels niet gepost kunnen worden en de juiste foutmeldingen tonen.")
    @DisplayName("Verhaal posten met te lange titel toont fout")
    void submit_title_exceeding_maximum_shows_error() {
        Allure.step("Post verhaal met te lange titel", () -> {
            String longTitle = repeat('A', 102);
            pages.writeStory.writeStory(longTitle, storyContent);
        });

        Allure.step("Controleer foutmelding voor te lange titel", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Verwachte foutmelding voor te lange titel")
                                .that(pages.writeStory.getMessageToast())
                                .contains("Titel mag maximaal 100 tekens bevatten.")
                )
        );
    }

    @Test
    @Order(5)
    @Story("Verhaal posten met speciale tekens in titel")
    @Description("Controleert dat verhalen met speciale tekens in de titel niet gepost kunnen worden en de juiste foutmeldingen tonen.")
    @DisplayName("Verhaal posten met speciale tekens in titel toont fout")
    void submit_title_with_special_chars_shows_error() {
        Allure.step("Post verhaal met speciale tekens in titel", () -> {
            pages.writeStory.writeStory("Invalid@Titel!", storyContent);
        });

        Allure.step("Controleer foutmelding voor speciale tekens in titel", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Verwachte foutmelding voor speciale tekens in titel")
                                .that(pages.writeStory.getMessageToast())
                                .contains("Titel mag alleen letters, cijfers en spaties bevatten.")
                )
        );
    }

    @Test
    @Order(6)
    @Story("Verhaal posten met lege inhoud")
    @Description("Controleert dat verhalen zonder inhoud niet gepost kunnen worden en de juiste foutmeldingen tonen.")
    @DisplayName("Verhaal posten met lege inhoud toont fout")
    void submit_empty_content_shows_error() {
        Allure.step("Post verhaal met lege inhoud", () -> {
            pages.writeStory.writeStory("Geldige Titel", "");
        });

        Allure.step("Controleer foutmelding voor lege inhoud", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Verwachte foutmelding voor lege inhoud")
                                .that(pages.writeStory.getMessageToast())
                                .contains("Vul je verhaal in.")
                )
        );
    }

    @Test
    @Order(7)
    @Story("Verhaal posten met alleen spaties in inhoud")
    @Description("Controleert dat verhalen met alleen spaties in de inhoud niet gepost kunnen worden en de juiste foutmeldingen tonen.")
    @DisplayName("Verhaal posten met alleen spaties in inhoud toont fout")
    void submit_spaces_only_content_shows_error() {
        Allure.step("Post verhaal met alleen spaties in inhoud", () -> {
            pages.writeStory.writeStory("Geldige Titel", "   ");
        });

        Allure.step("Controleer foutmelding voor inhoud met alleen spaties", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Verwachte foutmelding voor inhoud met alleen spaties")
                                .that(pages.writeStory.getMessageToast())
                                .contains("Vul je verhaal in.")
                )
        );
    }

    @Test
    @Order(8)
    @Story("Verhaal posten met titel op maximale lengte")
    @Description("Controleert dat verhalen met een titel op de maximale lengte succesvol gepost kunnen worden.")
    @DisplayName("Verhaal posten met titel op maximale lengte slaagt")
    void submit_title_at_max_boundary_publishes() {
        Allure.step("Post verhaal met titel op maximale lengte", () -> {
            String title = repeat('T', 100);
            pages.writeStory.writeStory(title, storyContent);
        });

        Allure.step("Controleer redirect naar homepage", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Titel op maximum moet slagen en naar home routeren")
                                .that(pages.writeStory.isOnHomePage())
                                .isTrue()
                )
        );
    }

    @Test
    @Order(9)
    @Story("Verhaal posten met titel net onder maximale lengte")
    @Description("Controleert dat verhalen met een titel net onder de maximale lengte succesvol gepost kunnen worden.")
    @DisplayName("Verhaal posten met titel net onder maximale lengte slaagt")
    void submit_title_near_max_boundary_publishes() {
        Allure.step("Post verhaal met titel net onder maximale lengte", () -> {
            String title = repeat('T', 99);
            pages.writeStory.writeStory(title, storyContent);
        });

        Allure.step("Controleer redirect naar homepage", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Titel net onder maximum moet slagen en naar home routeren")
                                .that(pages.writeStory.isOnHomePage())
                                .isTrue()
                )
        );
    }

    @Test
    @Order(10)
    @Story("Verhaal posten met titel op minimale lengte")
    @Description("Controleert dat verhalen met een titel op de minimale lengte succesvol gepost kunnen worden.")
    @DisplayName("Verhaal posten met titel op minimale lengte slaagt")
    void submit_title_exact_minimum_publishes() {
        Allure.step("Post verhaal met titel op minimale lengte", () -> {
            String title = repeat('M', 5);
            pages.writeStory.writeStory(title, storyContent);
        });

        Allure.step("Controleer redirect naar homepage", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Titel op minimum moet slagen en naar home routeren")
                                .that(pages.writeStory.isOnHomePage())
                                .isTrue()
                )
        );
    }

    @Test
    @Order(11)
    @Story("Verhaal posten met titel met leestekens")
    @Description("Controleert dat verhalen met een titel die alleen uit leestekens bestaat niet gepost kunnen worden en de juiste foutmeldingen tonen.")
    @DisplayName("Verhaal posten met titel met leestekens toont fout")
    void submit_title_with_punctuation_valid_length_shows_error() {
        Allure.step("Post verhaal met titel met leestekens", () -> {
            String title = repeat('!', 10);
            pages.writeStory.writeStory(title, storyContent);
        });

        Allure.step("Controleer foutmelding voor titel met leestekens", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Verwachte foutmelding voor titel met leestekens")
                                .that(pages.writeStory.getMessageToast())
                                .contains("Titel mag alleen letters, cijfers en spaties bevatten.")
                )
        );
    }

    @Test
    @Order(12)
    @Story("Verhaal posten met te korte titel met leestekens")
    @Description("Controleert dat verhalen met een titel die alleen uit leestekens bestaat en te kort is niet gepost kunnen worden en de juiste foutmeldingen tonen.")
    @DisplayName("Verhaal posten met te korte titel met leestekens toont fout")
    void submit_title_with_punctuation_too_short_shows_error() {
        Allure.step("Post verhaal met te korte titel met leestekens", () -> {
            String title = repeat('!', 3);
            pages.writeStory.writeStory(title, storyContent);
        });

        Allure.step("Controleer foutmelding voor te korte titel met leestekens", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Verwachte foutmelding voor te korte titel met leestekens")
                                .that(pages.writeStory.getMessageToast())
                                .contains("Titel mag alleen letters, cijfers en spaties bevatten.")
                )
        );
    }

    @Test
    @Order(13)
    @Story("Verhaal posten met te lange titel met leestekens")
    @Description("Controleert dat verhalen met een titel die alleen uit leestekens bestaat en te lang is niet gepost kunnen worden en de juiste foutmeldingen tonen.")
    @DisplayName("Verhaal posten met te lange titel met leestekens toont fout")
    void submit_title_with_punctuation_too_long_shows_error() {
        Allure.step("Post verhaal met te lange titel met leestekens", () -> {
            String title = repeat('!', 101);
            pages.writeStory.writeStory(title, storyContent);
        });

        Allure.step("Controleer foutmelding voor te lange titel met leestekens", () ->
                assertWithScreenshot(() ->
                        assertWithMessage("Verwachte foutmelding voor te lange titel met leestekens")
                                .that(pages.writeStory.getMessageToast())
                                .contains("Titel mag alleen letters, cijfers en spaties bevatten.")
                )
        );
    }

    @Test
    @Order(14)
    @Story("Zoeken en bekijken van verhaal details door tweede gebruiker")
    @Description("Controleert dat een tweede gebruiker een verhaal kan zoeken en alle details correct worden weergegeven.")
    @DisplayName("Tweede gebruiker kan verhaal zoeken en details bekijken")
    void second_user_can_search_and_view_story_details() throws InterruptedException {
        Allure.step("Eerste gebruiker schrijft een verhaal", () -> {
            pages.writeStory.writeStory(storyTitle, storyContent);

            assertWithScreenshot(() ->
                    assertWithMessage("Publicatie moet slagen")
                            .that(pages.writeStory.isOnHomePage())
                            .isTrue()
            );
        });

        Allure.step("Log uit als eerste gebruiker", () -> {
            pages.settings.hoverOverUsername();
            pages.settings.navigateToSettings();
            pages.settings.clickLogout();
        });

        Allure.step("Tweede gebruiker registreren en inloggen", () -> {
            String unique2 = unique + "2";
            String user2 = "StoryUser" + unique2;
            String email2 = "story2+" + unique2 + "@mail.com";
            pages.register.navigateTo();
            pages.register.registerStepOne(user2, email2, "Password2!", "Password2!");
            pages.register.registerStepTwo("3500", "Depressie");
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
        });

        Allure.step("Zoek en open het verhaal via de detailpagina", () -> {
            pages.storyView.openStoryByTitle(storyTitle);
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        });

        Allure.step("Controleer dat alle data correct is", () -> {
            assertWithScreenshot(() -> {
                assertWithMessage("Titel moet overeenkomen")
                        .that(pages.storyDetail.getStoryTitle())
                        .isEqualTo(storyTitle);

                assertWithMessage("Inhoud moet overeenkomen")
                        .that(pages.storyDetail.getStoryContent())
                        .isEqualTo(storyContent);

                assertWithMessage("Auteur moet overeenkomen")
                        .that(pages.storyDetail.getStoryAuthor())
                        .isEqualTo("StoryUser" + unique);
            });
        });

        Allure.step("Cleanup: verwijder beide accounts", () -> {
            pages.settings.hoverOverUsername();
            pages.settings.navigateToSettings();
            pages.settings.clickLogout();

            pages.login.navigateTo();
            pages.login.login("story+" + unique + "@mail.com", password);
            pages.settings.hoverOverUsername();
            pages.settings.navigateToSettings();
            pages.settings.deleteAccount(password);
        });
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    @AfterEach
    public void teardown() {
        try {
            if (pages.settings.isUserLoggedIn()) {
                pages.settings.hoverOverUsername();
                pages.settings.navigateToSettings();
                pages.settings.deleteAccount(password);
            }
        } catch (Exception ignored) {}

        pages.closeBrowser();
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
