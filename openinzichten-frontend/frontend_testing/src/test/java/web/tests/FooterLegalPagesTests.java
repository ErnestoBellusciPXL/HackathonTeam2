package web.tests;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertWithMessage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import java.util.UUID;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Footer - juridische pagina’s")
public class FooterLegalPagesTests {
    private AllOpenInzichtPages pages;
    private String validEmail = "login@mail.com";
    private String validPassword = "Password1!";
    private String validUsername = "TestUser";
    private String validPostalCode = "3500";
    private String validCondition = "Depressie";

    @BeforeEach
    @Description("Initialiseer browser en testgebruiker")
    public void setup() {
        pages = new AllOpenInzichtPages();

        // Generate unique user credentials for each test run to avoid collisions
        String uuid = UUID.randomUUID().toString().replace("-", "");
        validUsername = "TestUser" + uuid.substring(0, 8);
        validEmail = "login+" + uuid.substring(0, 8) + "@mail.com";

        Allure.step("Navigeer naar homepage", () -> {
            pages.home.navigateTo();
        });
    }

    @AfterEach
    @Description("Verwijder testaccount indien nodig en sluit browser")
    public void tearDown() {
        try {
            if (pages.settings.isUserLoggedIn()) {
                Allure.step("Verwijder testaccount", () -> {
                    pages.login.navigateTo();
                    pages.login.login(validEmail, validPassword);
                    pages.settings.hoverOverUsername();
                    pages.settings.navigateToSettings();
                    pages.settings.deleteAccount(validPassword);
                });
            }
        } catch (Exception ignored) {}

        if (pages != null) {
            pages.closeBrowser();
        }
    }

    @Test
    @Order(1)
    @Story("Gebruikersvoorwaarden")
    @Description("Gebruikersvoorwaarden zijn toegankelijk zonder login")
    @DisplayName("Terms – niet ingelogd")
    public void navigateToTermsPageUserNotLoggedIn() {
        Allure.step("Scroll naar Gebruikersvoorwaarden link", () -> {
            sleep(1000);
            pages.terms.scrollDownToTermsLink();
        });

        Allure.step("Navigeer naar Gebruikersvoorwaarden pagina", () -> {
            pages.terms.navigateToTermsPage();
        });

        Allure.step("Valideer Gebruikersvoorwaarden pagina", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Moet op Gebruikersvoorwaarden pagina zijn")
                            .that(pages.terms.isOnTermsPage())
                            .isTrue()
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("'Laatst bijgewerkt' moet zichtbaar zijn op Gebruikersvoorwaarden")
                            .that(pages.terms.isLastUpdatedPresent())
                            .isTrue()
            );
        });
    }

    @Test
    @Order(2)
    @Story("Gebruikersvoorwaarden")
    @Description("Gebruikersvoorwaarden zijn toegankelijk met login")
    @DisplayName("Terms – ingelogd")
    public void navigateToTermsPageUserLoggedIn() {
        Allure.step("Registreer en login gebruiker", () -> {
            registerUser();
            loginUser();
        });

        Allure.step("Scroll naar Gebruikersvoorwaarden link", () -> {
            sleep(1000);
            pages.terms.scrollDownToTermsLink();
        });

        Allure.step("Navigeer naar Gebruikersvoorwaarden pagina", () -> {
            pages.terms.navigateToTermsPage();
        });

        Allure.step("Valideer Gebruikersvoorwaarden pagina", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Moet op Gebruikersvoorwaarden pagina zijn")
                            .that(pages.terms.isOnTermsPage())
                            .isTrue()
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("'Laatst bijgewerkt' moet zichtbaar zijn op Gebruikersvoorwaarden")
                            .that(pages.terms.isLastUpdatedPresent())
                            .isTrue()
            );
        });
    }

    @Test
    @Order(3)
    @Story("Privacybeleid")
    @Description("Privacybeleid is toegankelijk zonder login")
    @DisplayName("Privacy – niet ingelogd")
    public void navigateToPrivacyPageUserNotLoggedIn() {
        Allure.step("Scroll naar Privacybeleid link", () -> {
            sleep(1000);
            pages.privacyPolicy.scrollDownToPrivacyLink();
        });

        Allure.step("Navigeer naar Privacybeleid pagina", () -> {
            pages.privacyPolicy.navigateToPrivacyPage();
        });

        Allure.step("Valideer Privacybeleid pagina", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Moet op Privacybeleid pagina zijn")
                            .that(pages.privacyPolicy.isOnPrivacyPage())
                            .isTrue()
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("'Laatst bijgewerkt' moet zichtbaar zijn op Privacybeleid")
                            .that(pages.privacyPolicy.isLastUpdatedPresent())
                            .isTrue()
            );
        });
    }

    @Test
    @Order(4)
    @Story("Privacybeleid")
    @Description("Privacybeleid is toegankelijk met login")
    @DisplayName("Privacy – ingelogd")
    public void navigateToPrivacyPageUserLoggedIn() {
        Allure.step("Registreer en login gebruiker", () -> {
            registerUser();
            loginUser();
        });

        Allure.step("Scroll naar Privacybeleid link", () -> {
            sleep(1000);
            pages.privacyPolicy.scrollDownToPrivacyLink();
        });

        Allure.step("Navigeer naar Privacybeleid pagina", () -> {
            pages.privacyPolicy.navigateToPrivacyPage();
        });

        Allure.step("Valideer Privacybeleid pagina", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Moet op Privacybeleid pagina zijn")
                            .that(pages.privacyPolicy.isOnPrivacyPage())
                            .isTrue()
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("'Laatst bijgewerkt' moet zichtbaar zijn op Privacybeleid")
                            .that(pages.privacyPolicy.isLastUpdatedPresent())
                            .isTrue()
            );
        });
    }

    @Test
    @Order(5)
    @Story("Gedragscode")
    @Description("Gedragscode is toegankelijk zonder login")
    @DisplayName("Gedragscode – niet ingelogd")
    public void navigateToCodeOfConductPageUserNotLoggedIn() {
        Allure.step("Scroll naar Gedragscode link", () -> {
            sleep(1000);
            pages.codeOfConductPage.scrollDownToCodeOfConductLink();
        });

        Allure.step("Navigeer naar Gedragscode pagina", () -> {
            pages.codeOfConductPage.navigateToCodeOfConductPage();
        });

        Allure.step("Valideer Gedragscode pagina", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Moet op Gedragscode pagina zijn")
                            .that(pages.codeOfConductPage.isOnCodeOfConductPage())
                            .isTrue()
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("'Laatst bijgewerkt' moet zichtbaar zijn op Gedragscode")
                            .that(pages.codeOfConductPage.isLastUpdatedPresent())
                            .isTrue()
            );
        });
    }

    @Test
    @Order(6)
    @Story("Gedragscode")
    @Description("Gedragscode is toegankelijk met login")
    @DisplayName("Gedragscode – ingelogd")
    public  void navigateToCodeOfConductPageUserLoggedIn() {
        Allure.step("Registreer en login gebruiker", () -> {
            registerUser();
            loginUser();
        });

        Allure.step("Scroll naar Gedragscode link", () -> {
            sleep(1000);
            pages.codeOfConductPage.scrollDownToCodeOfConductLink();
        });

        Allure.step("Navigeer naar Gedragscode pagina", () -> {
            pages.codeOfConductPage.navigateToCodeOfConductPage();
        });

        Allure.step("Valideer Gedragscode pagina", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Moet op Gedragscode pagina zijn")
                            .that(pages.codeOfConductPage.isOnCodeOfConductPage())
                            .isTrue()
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("'Laatst bijgewerkt' moet zichtbaar zijn op Gedragscode")
                            .that(pages.codeOfConductPage.isLastUpdatedPresent())
                            .isTrue()
            );
        });
    }

    private void registerUser() {
        pages.register.navigateTo();
        pages.register.registerStepOne(validUsername, validEmail, validPassword, validPassword);
        pages.register.registerStepTwo(validPostalCode, validCondition);
        pages.settings.logout();
    }

    private void loginUser() {
        pages.login.navigateTo();
        pages.login.login(validEmail, validPassword);
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }
}