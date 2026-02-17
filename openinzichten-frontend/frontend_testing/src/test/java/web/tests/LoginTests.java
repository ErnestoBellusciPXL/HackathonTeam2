package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Login Functionaliteit")
public class LoginTests {
    private AllOpenInzichtPages pages;
    private String toastMessage;
    private String succesMessage = "gelukt";
    private String fillInAllFieldsMessage = "Vul alle velden in";
    private String invalidEmailPasswordCombinationMessage = "Combinatie van e-mailadres en wachtwoord is ongeldig";
    private String validEmail;
    private String validPassword = "Password1!";
    private String uniqueUser;


    @BeforeEach
    @Step("Start browser, maak unieke gebruiker aan en navigeer naar de loginpagina")
    public void setup() {
        pages = new AllOpenInzichtPages();
        // Use a unique user/email per test run
        String unique = String.valueOf(System.currentTimeMillis());
        uniqueUser = "TestUser" + unique;
        validEmail = "login+" + unique + "@mail.com";

        registerBeforeLogin();

        pages.login.navigateTo();
    }

    @Test
    @Order(1)
    @Story("Succesvol inloggen")
    @Description("Controleert dat een gebruiker succesvol kan inloggen met geldige credentials.")
    @DisplayName("Testen van succesvolle login met geldige gegevens")
    public void testValidLogin() {
        Allure.step("Voer login uit met geldige gegevens", () -> {
            pages.login.loginAndWait(validEmail, validPassword, java.time.Duration.ofSeconds(6));
        });

        Allure.step("Lees toast melding", () -> {
            toastMessage = pages.login.getMessageToast();
        });

        Allure.step("Valideer toast melding", () -> {
            try{
                assertThat(toastMessage).contains(succesMessage);
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    @Test
    @Order(2)
    @Story("Login mislukt — e-mailadres leeg")
    @Description("Controleert dat het systeem een foutmelding toont wanneer het e-mailadres niet is ingevuld.")
    @DisplayName("Testen van login met leeg e-mailadres")
    public void testLoginWithMailEmpty() {
        Allure.step("Inloggen met leeg e-mailadres", () -> {
            pages.login.login("", validPassword);
        });

        Allure.step("Lees toastmelding", () -> {
            toastMessage = pages.login.getMessageToast();
        });

        Allure.step("Valideer foutmelding", () -> {
            try{
                assertThat(toastMessage).contains(fillInAllFieldsMessage);
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    @Test
    @Order(3)
    @Story("Login mislukt — wachtwoord leeg")
    @Description("Controleert dat het systeem een foutmelding toont wanneer het wachtwoord niet ingevuld is.")
    @DisplayName("Testen van login met leeg wachtwoord")
    public void testLoginWithPasswordEmpty() {
        Allure.step("Inloggen met leeg wachtwoord", () -> {
            pages.login.login("t@m.c", "");
        });

        Allure.step("Lees toastmelding", () -> {
            toastMessage = pages.login.getMessageToast();
        });

        Allure.step("Valideer foutmelding", () -> {
            try{
                assertThat(toastMessage).contains(fillInAllFieldsMessage);
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    @Test
    @Order(4)
    @Story("Login mislukt — onbekend e-mailadres")
    @Description("Controleert dat login mislukt wanneer een onbekend e-mailadres wordt gebruikt.")
    @DisplayName("Testen van login met niet-geregistreerd e-mailadres")
    public void testLoginWithNonRegisteredEmail() {
        Allure.step("Inloggen met niet-geregistreerd e-mailadres", () -> {
            pages.login.login("other@mail.com", validPassword);
        });

        Allure.step("Lees toastmelding", () -> {
            toastMessage = pages.login.getMessageToast();
        });

        Allure.step("Valideer foutmelding", () -> {
            try{
                assertThat(toastMessage)
                        .contains(invalidEmailPasswordCombinationMessage);
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    @Test
    @Order(5)
    @Story("Login mislukt — fout wachtwoord")
    @Description("Controleert dat een fout wachtwoord een correcte foutmelding geeft.")
    @DisplayName("Testen van login met onjuist wachtwoord")
    public void testLoginWithWrongPassword() {
        Allure.step("Inloggen met fout wachtwoord", () -> {
            pages.login.login("t@m.c", "WrongPassword!");
        });

        Allure.step("Lees toastmelding", () -> {
            toastMessage = pages.login.getMessageToast();
        });

        Allure.step("Valideer foutmelding", () -> {
            try{
                assertThat(toastMessage)
                        .contains(invalidEmailPasswordCombinationMessage);
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    @Test
    @Order(6)
    @Story("Login mislukt — beide velden ongeldig")
    @Description("Controleert dat login mislukt wanneer zowel e-mail als wachtwoord ongeldig zijn.")
    @DisplayName("Testen van login met zowel ongeldig e-mailadres als wachtwoord")
    public void testLoginWithBothFieldsInvalid() {
        Allure.step("Inloggen met foutieve mail en fout wachtwoord", () -> {
            pages.login.login("other@mail.com", "WrongPassword!");
        });

        Allure.step("Lees toastmelding", () -> {
            toastMessage = pages.login.getMessageToast();
        });

        Allure.step("Valideer foutmelding", () -> {
            try {
                assertThat(toastMessage)
                        .contains(invalidEmailPasswordCombinationMessage);
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    @Test
    @Order(7)
    @Story("Navigatie na succesvolle login")
    @Description("Controleert dat een succesvolle login leidt naar de homepage.")
    @DisplayName("Testen van navigatie naar homepage na succesvolle login")
    public void testSuccesfulLoginNavigatesToHomepage() {
        Allure.step("Inloggen", () -> {
            pages.login.loginAndWait(validEmail, validPassword, java.time.Duration.ofSeconds(6));
        });

        Allure.step("Valideer dat we op de homepage zijn", () -> {
            try{
                assertThat(pages.login.isOnHomePage()).isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    @AfterEach
    public void teardown() {
        if (pages != null) {
            try {
                // If not logged in, try to login (use loginAndWait)
                if (!pages.settings.isUserLoggedIn()) {
                    pages.login.navigateTo();
                    pages.login.loginAndWait(validEmail, validPassword, java.time.Duration.ofSeconds(6));
                }

                // If logged in now, navigate to settings and delete account
                if (pages.settings.isUserLoggedIn()) {
                    try {
                        pages.settings.hoverOverUsername();
                        pages.settings.navigateToSettings();
                        pages.settings.deleteAccountAndWait(validPassword, "Je account is succesvol verwijderd");
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            pages.closeBrowser();
        }
    }

    private void registerBeforeLogin() {
        pages.register.navigateTo();
        pages.register.registerStepOne(uniqueUser, validEmail, validPassword, validPassword);
        pages.register.registerStepTwo("3500", "Depressie");

        // Wait until registration flow finishes and we are on the home page or the user UI appears
        try {
            String normalizedBase = pages.register.baseurl.endsWith("/") ? pages.register.baseurl : pages.register.baseurl + "/";
            new WebDriverWait(pages.getDriver(), java.time.Duration.ofSeconds(12))
                    .until(d -> d.getCurrentUrl().startsWith(normalizedBase)
                            || !d.findElements(By.id("HoverUsername")).isEmpty());

            // If registration logged us in, log out so the login tests can exercise the flow.
            if (!pages.getDriver().findElements(By.id("HoverUsername")).isEmpty()) {
                pages.settings.hoverOverUsername();
                pages.settings.logout();
            }
        } catch (Exception ignored) {}
    }


    public void takeScreenShot(){
        Object drv = pages.getDriver();
        byte[] img = ((org.openqa.selenium.TakesScreenshot) drv).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        io.qameta.allure.Allure.addAttachment("screenshot", "image/png", new java.io.ByteArrayInputStream(img), "png");
    }
}
