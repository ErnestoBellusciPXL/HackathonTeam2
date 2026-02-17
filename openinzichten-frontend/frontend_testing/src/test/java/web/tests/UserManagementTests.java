package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Gebruikersbeheer Functionaliteit")
public class UserManagementTests {
    private AllOpenInzichtPages pages;
    private String testUsername;
    private String testEmail;
    private final String testPassword = "Password1!";

    @BeforeEach
    @Step("Setup testdata: registreer testgebruiker en login als admin")
    public void setup() throws InterruptedException {
        pages = new AllOpenInzichtPages();

        Allure.step("Genereer unieke gebruikersnaam en email", () -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            testUsername = "aaTESTUSER" + timestamp;
            testEmail = "aaTESTUSER" + timestamp + "@test.com";
        });

        Allure.step("Registreer testgebruiker", () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(testUsername, testEmail, testPassword, testPassword);
            pages.register.registerStepTwo("3500", "Depressie");
            Thread.sleep(1200);
        });

        Allure.step("Logout testgebruiker", () -> {
            if (pages.settings.isUserLoggedIn()) {
                pages.settings.logout();
                Thread.sleep(800);
            }
        });

        Allure.step("Login als admin", () -> {
            pages.login.navigateTo();
            boolean loggedIn = pages.login.loginAndWait("admin@openinzicht.be", "0penInzicht?1PXL", Duration.ofSeconds(8));
            if (!loggedIn) {
                throw new AssertionError("Admin login mislukt");
            }
            Thread.sleep(800);
        });

        Allure.step("Navigeer naar Admin Dashboard", () -> {
            pages.admin.ensureOnAdmin();
            Thread.sleep(800);
        });

        Allure.step("Open gebruikers tab", () -> {
            pages.admin.openUsersTab();
            Thread.sleep(1000);
        });
    }

    @Test
    @Order(1)
    @Story("Gebruiker detailpagina openen en controleren")
    @Description("Controleert dat de gebruiker detailpagina correct wordt geopend met alle vereiste elementen.")
    @DisplayName("Gebruiker detailpagina toont vereiste elementen")
    public void testUserDetailPageShowsRequiredElements() throws InterruptedException {
//        Allure.step("Controleer dat er beheren knoppen zichtbaar zijn", () -> {
//            assertWithScreenshot(() ->
//                    assertWithMessage("Er moeten beheren knoppen beschikbaar zijn")
//                            .that(pages.admin.getBeherenButtonCount())
//                            .isGreaterThan(0));
//        });

        Allure.step("Controleer dat de 'Beheren' kolomkop aanwezig is", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("Kolomkop 'Beheren' moet aanwezig zijn in de gebruikers tabel")
                            .that(pages.admin.isBeherenHeaderPresent())
                            .isTrue()
            );
        });

        Allure.step("Controleer dat eerste 'Beheren' knop klikbaar is", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("Eerste 'Beheren' knop moet klikbaar zijn")
                            .that(pages.admin.canClickBeherenButton(0))
                            .isTrue()
            );
        });

        Allure.step("Klik op eerste 'Beheren' knop", () -> {
            pages.admin.clickBeherenButton(0);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
        });

        Allure.step("Controleer dat we op de user detail pagina zijn", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("URL moet /admin/users/ bevatten")
                            .that(pages.adminUserDetail.isOnUserDetailPage())
                            .isTrue());
        });

        Allure.step("Controleer dat gebruikersnaam aanwezig en niet leeg is", () -> {
            assertWithScreenshot(() -> {
                String username = pages.adminUserDetail.getUsername();
                assertWithMessage("Gebruikersnaam moet aanwezig zijn en lengte > 0 hebben")
                        .that(username.length())
                        .isGreaterThan(0);
            });
        });

        Allure.step("Controleer dat email aanwezig en niet leeg is", () -> {
            assertWithScreenshot(() -> {
                String email = pages.adminUserDetail.getEmail();
                assertWithMessage("Email moet aanwezig zijn en lengte > 0 hebben")
                        .that(email.length())
                        .isGreaterThan(0);
            });
        });

        Allure.step("Controleer dat er minstens één condition aanwezig is", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("Er moet minstens één condition-item aanwezig zijn")
                            .that(pages.adminUserDetail.getConditionCount())
                            .isGreaterThan(0));
        });

        Allure.step("Controleer dat user-story-title element aanwezig is", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("user-story-title element moet aanwezig zijn")
                            .that(pages.adminUserDetail.isUserStoryTitlePresent())
                            .isTrue());
        });

        Allure.step("Controleer dat user-story element aanwezig is", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("user-story element moet aanwezig zijn")
                            .that(pages.adminUserDetail.isUserStoryPresent())
                            .isTrue());
        });

        Allure.step("Controleer dat status element aanwezig is", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("status element moet aanwezig zijn")
                            .that(pages.adminUserDetail.isStatusPresent())
                            .isTrue());
        });

        Allure.step("Controleer dat toggle-user-activation knop aanwezig is", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("toggle-user-activation knop moet aanwezig zijn")
                            .that(pages.adminUserDetail.isToggleUserActivationButtonPresent())
                            .isTrue());
        });
    }

    @AfterEach
    @Step("Verwijder testaccount en sluit browser")
    public void tearDown() {
        try {
            // Logout admin if logged in
            if (pages.settings.isUserLoggedIn()) {
                pages.settings.logout();
                Thread.sleep(800);
            }
        } catch (Exception ignored) {}

        try {
            // Login as test user and delete account
            pages.login.navigateTo();
            pages.login.loginAndWait(testEmail, testPassword, Duration.ofSeconds(6));
            Thread.sleep(800);
            
            if (pages.settings.isUserLoggedIn()) {
                try {
                    pages.settings.hoverOverUsername();
                    pages.settings.navigateToSettings();
                    pages.settings.deleteAccountAndWait(testPassword, "Je account is succesvol verwijderd");
                } catch (Exception ignored) {}
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

