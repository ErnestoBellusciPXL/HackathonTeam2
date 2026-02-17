package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Registratie info-tooltips")
public class RegisterInfoTooltipTests {

    private AllOpenInzichtPages pages;
    private final String password = "StrongPass1!";

    @BeforeEach
    @Step("Open browser")
    void setUp() {
        pages = new AllOpenInzichtPages();
    }

    @AfterEach
    @Step("Sluit browser")
    void tearDown() {
        if (pages != null) pages.closeBrowser();
    }

    private String unique() {
        return String.valueOf(System.currentTimeMillis()).substring(6);
    }


    private void goToRegisterStepOne() {
        Allure.step("Going to register page", () -> {
            pages.register.navigateTo();
        });
    }

    private void goToRegisterStepTwo() {
        String suffix = unique();

        Allure.step("Vul registratie stap 1 in", () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(
                    "InfoUser" + suffix,
                    "info" + suffix + "@example.com",
                    password,
                    password
            );
        });

        assertWithScreenshot(() ->
                assertTrue(
                        pages.register.waitForSecondForm(),
                        "Stap 2 van registratie werd niet bereikt"
                )
        );
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
    @Story("Gebruikersnaam tooltip")
    @Description("Toont correcte info bij hover over gebruikersnaam info-icoon")
    @DisplayName("Gebruikersnaam info-icoon toont correcte tooltip")
    void infoIcon_username_showsExpectedTooltip() {
        goToRegisterStepOne();

        String tooltip = pages.register.getUsernameTooltipText();

        Allure.step("Hover over Gebruikersnaam tooltip", () -> {
            assertWithScreenshot(() ->
                    assertEquals(
                            "2–40 tekens, alleen letters en cijfers. Geen spaties.",
                            tooltip,
                            "Gebruikersnaam tooltip klopt niet"
                    )
            );
        });

    }

    @Test
    @Order(2)
    @Story("Wachtwoord tooltip")
    @Description("Toont correcte info bij hover over wachtwoord info-icoon")
    @DisplayName("Wachtwoord info-icoon toont correcte tooltip")
    void infoIcon_password_showsExpectedTooltip() {
        goToRegisterStepOne();

        String tooltip = pages.register.getPasswordTooltipText();

        Allure.step("Hover over Wachtwoord tooltip", () -> {
            assertWithScreenshot(() ->
                    assertEquals(
                            "Min. 8 tekens, minstens 1 hoofdletter, 1 cijfer en 1 speciaal teken (!@#$%^&*?).",
                            tooltip,
                            "Wachtwoord tooltip klopt niet"
                    )
            );
        });
    }

    @Test
    @Order(3)
    @Story("Postcode tooltip")
    @Description("Toont correcte privacy-info bij postcode in registratie stap 2")
    @DisplayName("Postcode info-icoon toont correcte tooltip op tweede formulier")
    void infoIcon_zipcode_showsExpectedTooltipOnSecondForm() {
        goToRegisterStepTwo();

        String tooltip = pages.register.getZipcodeTooltipText();

        Allure.step("Hover over Postcode tooltip", () -> {
            assertWithScreenshot(() ->
                    assertEquals(
                            "Je gegevens worden veilig en vertrouwelijk verwerkt. Deze worden alleen gebruikt om lotgenoten in uw buurt te tonen.",
                            tooltip,
                            "Postcode tooltip klopt niet"
                    )
            );
        });
    }

    @Test
    @Order(4)
    @Story("Aandoening tooltip")
    @Description("Toont correcte privacy-info bij aandoening in registratie stap 2")
    @DisplayName("Aandoening info-icoon toont correcte tooltip op tweede formulier")
    void infoIcon_condition_showsExpectedTooltipOnSecondForm() {
        goToRegisterStepTwo();

        String tooltip = pages.register.getConditionTooltipText();

        Allure.step("Hover over Aandoening tooltip", () -> {
            assertWithScreenshot(() ->
                    assertEquals(
                            "Je gegevens worden veilig en vertrouwelijk verwerkt. Deze worden alleen gebruikt om een heatmap van aandoeningen te tonen.",
                            tooltip,
                            "Aandoening tooltip klopt niet"
                    )
            );
        });
    }
}
