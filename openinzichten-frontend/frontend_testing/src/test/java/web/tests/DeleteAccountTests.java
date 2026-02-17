package web.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import java.time.Duration;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Account verwijderen")
public class DeleteAccountTests {
    private AllOpenInzichtPages pages;
    private String expectedToastSuccesMessage = "Je account is succesvol verwijderd";
    private String expectedToastErrorMessage = "Het verwijderen van je account is mislukt";
    private String validPassword = "Password1!";
    private String invalidPassword = "WrongPassword!";
    private String emptyPassword = "";
    private String uniqueUser;
    private String uniqueEmail;

    @BeforeEach
    @Description("Registreer een nieuwe gebruiker voor de test")
    public void setup() {
        pages = new AllOpenInzichtPages();
        // Use a unique username/email per test run to avoid conflicts
        String unique = String.valueOf(System.currentTimeMillis());
        uniqueUser = "DeleteAccountUserr" + unique;
        uniqueEmail = "deleteAccountUserr+" + unique + "@mail.com";

        Allure.step("Registreer testgebruiker", () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(uniqueUser, uniqueEmail, validPassword, validPassword);
            pages.register.registerStepTwo("3500", "Depressie");
        });
    }

    @Test
    @Order(1)
    @Story("Account verwijderen met geldig wachtwoord")
    @Description("Controleert dat een gebruiker zijn account kan verwijderen met een correct wachtwoord")
    @DisplayName("Account verwijderen met geldig wachtwoord")
    public void deleteAccount_validPassword_accountDeleted() {
        Allure.step("Verwijder account met geldig wachtwoord", () -> {
            boolean result = pages.settings.deleteAccountAndWait(
                    validPassword,
                    expectedToastSuccesMessage
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Succes-toast moet zichtbaar zijn bij correct wachtwoord")
                            .that(result || pages.settings.isToastMessagePresent(expectedToastSuccesMessage))
                            .isTrue()
            );
        });
    }

    @Test
    @Order(2)
    @Story("Account verwijderen met fout wachtwoord")
    @Description("Controleert dat het verwijderen van een account faalt bij een fout wachtwoord")
    @DisplayName("Account verwijderen met fout wachtwoord")
    public void deleteAccount_invalidPassword_accountNotDeleted() {
        Allure.step("Probeer account te verwijderen met fout wachtwoord", () -> {
            boolean result = pages.settings.deleteAccountAndWait(
                    invalidPassword,
                    expectedToastErrorMessage
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Error-toast moet zichtbaar zijn bij fout wachtwoord")
                            .that(result || pages.settings.isToastMessagePresent(expectedToastErrorMessage))
                            .isTrue()
            );
        });

        Allure.step("Cleanup: verwijder account alsnog met correct wachtwoord", () -> {
            try {
                pages.settings.deleteAccountAndWait(validPassword, expectedToastSuccesMessage);
            } catch (Exception ignored) {}
        });
    }

    @Test
    @Order(3)
    @Story("Account verwijderen met leeg wachtwoord")
    @Description("Controleert dat het verwijderen van een account faalt bij een leeg wachtwoord")
    @DisplayName("Account verwijderen met leeg wachtwoord")
    public void deleteAccount_emptyPassword_accountNotDeleted() {
        Allure.step("Probeer account te verwijderen met leeg wachtwoord", () -> {
            boolean result = pages.settings.deleteAccountAndWait(
                    emptyPassword,
                    expectedToastErrorMessage
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Error-toast moet zichtbaar zijn bij leeg wachtwoord")
                            .that(result || pages.settings.isToastMessagePresent(expectedToastErrorMessage))
                            .isTrue()
            );
        });

        Allure.step("Cleanup: verwijder account alsnog met correct wachtwoord", () -> {
            try {
                pages.settings.deleteAccountAndWait(validPassword, expectedToastSuccesMessage);
            } catch (Exception ignored) {}
        });
    }

    @AfterEach
    @Description("Verwijder testaccount indien deze nog bestaat en sluit browser")
    public void tearDown() {
        if (pages != null) {
            Allure.step("Controleer of account nog bestaat en verwijder indien nodig", () -> {
                try {
                    pages.login.navigateTo();
                    boolean loggedIn = pages.login.loginAndWait(
                            uniqueEmail,
                            validPassword,
                            Duration.ofSeconds(6)
                    );

                    if (loggedIn) {
                        try {
                            pages.settings.deleteAccountAndWait(
                                    validPassword,
                                    expectedToastSuccesMessage
                            );
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            });

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
