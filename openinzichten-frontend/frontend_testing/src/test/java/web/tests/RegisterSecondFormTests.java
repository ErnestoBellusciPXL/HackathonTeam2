package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Registratie tweede formulier validaties")
public class RegisterSecondFormTests {

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

    private String uniqueEmail(String prefix) {
        return prefix + System.currentTimeMillis() % 100000 + "@example.com";
    }

    private void fillStepOne(String username, String email) {
        Allure.step("Given: Stap 1 van registratie invullen", () -> {
            pages.register.navigateTo();
            pages.register.enterUsername(username);
            pages.register.enterEmail(email);
            pages.register.enterPassword(password);
            pages.register.enterConfirmPassword(password);
            pages.register.checkTerms();
            pages.register.clickRegisterNext();
        });

        assertWithScreenshot(() ->
                assertWithMessage("Stap 2 van registratie werd niet bereikt")
                        .that(pages.register.waitForSecondForm())
                        .isTrue()
        );
    }

    private void completeStepTwo(String postalCode, String condition) {
        Allure.step("When: Stap 2 van registratie invullen", () -> {
            pages.register.enterPostalCode(postalCode);
            if (condition != null) {
                try { pages.register.selectCondition(condition); } catch (Exception ignored) {}
            }
            pages.register.clickCompleteProfile();
        });
    }

    private void assertErrorToastAndNotHome() {
        assertWithScreenshot(() -> {
            assertWithMessage("Toast bericht ontbreekt of leeg bij foutieve invoer")
                    .that(pages.register.getMessageToast())
                    .isNotNull();
            assertWithMessage("Niet naar home navigeren bij validatiefout")
                    .that(pages.register.isOnHomePage())
                    .isFalse();
        });
    }

    private void assertNavigatedToHome() {
        assertWithScreenshot(() ->
                assertWithMessage("Verwacht navigatie naar home na succesvolle registratie")
                        .that(pages.register.isOnHomePage())
                        .isTrue()
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

    private void takeScreenShot() {
        Object drv = pages.getDriver();
        byte[] img = ((org.openqa.selenium.TakesScreenshot) drv)
                .getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        io.qameta.allure.Allure.addAttachment("screenshot", "image/png",
                new java.io.ByteArrayInputStream(img), "png");
    }

    @Test
    @Order(1)
    @Story("Lege aandoening")
    @Description("Zorgt dat foutmelding verschijnt als geen aandoening geselecteerd wordt")
    @DisplayName("Lege aandoening toont foutmelding en navigeert niet naar home")
    void secondForm_emptyCondition_showsErrorAndDoesNotNavigateHome() {
        fillStepOne("UserEmptyCond", uniqueEmail("emptycond"));
        completeStepTwo("3500", null);
        assertErrorToastAndNotHome();
    }

    @Test
    @Order(2)
    @Story("Ongeldige postcode")
    @Description("Zorgt dat foutmelding verschijnt bij ongeldig postcode")
    @DisplayName("Ongeldige postcode toont foutmelding en navigeert niet naar home")
    void secondForm_invalidPostcode_showsErrorAndDoesNotNavigateHome() {
        fillStepOne("UserInvalidPC", uniqueEmail("invalidpc"));
        completeStepTwo("!@#", "Diabetes");
        assertErrorToastAndNotHome();
    }

    @Test
    @Order(3)
    @Story("Lege postcode")
    @Description("Zorgt dat foutmelding verschijnt als postcode leeg is")
    @DisplayName("Lege postcode toont foutmelding en navigeert niet naar home")
    void secondForm_emptyPostcode_showsErrorAndDoesNotNavigateHome() {
        fillStepOne("UserEmptyPC", uniqueEmail("emptypc"));
        completeStepTwo("", "Diabetes");
        assertErrorToastAndNotHome();
    }

    @Test
    @Order(4)
    @Story("Ongeldige zipcode")
    @Description("Zorgt dat foutmelding verschijnt bij ongeldige zipcode")
    @DisplayName("Ongeldige zipcode toont foutmelding en navigeert niet naar home")
    void secondForm_invalidZipcode_showsErrorAndDoesNotNavigateHome() {
        fillStepOne("UserInvalidZip", uniqueEmail("invalidzip"));
        completeStepTwo("abc%%", "Diabetes");
        assertErrorToastAndNotHome();
    }

    @Test
    @Order(5)
    @Story("Lege zipcode")
    @Description("Zorgt dat foutmelding verschijnt als zipcode leeg is")
    @DisplayName("Lege zipcode toont foutmelding en navigeert niet naar home")
    void secondForm_emptyZipcode_showsErrorAndDoesNotNavigateHome() {
        fillStepOne("UserEmptyZip", uniqueEmail("emptyzip"));
        completeStepTwo("", "Diabetes");
        assertErrorToastAndNotHome();
    }

    @Test
    @Order(6)
    @Story("Happy path")
    @Description("Zorgt dat registratie succesvol is bij correcte invoer")
    @DisplayName("Succesvolle registratie navigeert naar home")
    void secondForm_happyPath_navigatesToHome() {
        fillStepOne("UserHappy", uniqueEmail("happy"));
        completeStepTwo("3500", "Diabetes");
        assertNavigatedToHome();
    }

}
