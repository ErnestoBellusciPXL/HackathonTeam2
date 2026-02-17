package web.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import org.openqa.selenium.support.ui.WebDriverWait;

import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Error pagina routing")
public class ErrorTests {
    private AllOpenInzichtPages pages;

    @BeforeEach
    @Description("Initialiseer browser en navigeer naar homepage")
    public void setup() {
        pages = new AllOpenInzichtPages();

        Allure.step("Navigeer naar homepage", () -> {
            pages.home.navigateTo();
        });    }

    @AfterEach
    @Description("Sluit browser na test")
    public void teardown() {
        if (pages != null) {
            pages.closeBrowser();
        }
    }

    @Test
    @Order(1)
    @Story("Route naar niet-bestaande pagina")
    @Description("Controleert dat het systeem automatisch naar de error page geroute wordt wanneer je naar een niet-bestaande URL navigeert.")
    @DisplayName("Niet-bestaande route toont error pagina")
    public void navigateToNonExistentPage() {
        Allure.step("Navigeer naar niet-bestaande route", () -> {
            pages.error.navigateTo("this-page-does-not-exist");
        });

        Allure.step("Valideer dat error pagina wordt getoond", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Gebruiker moet op de error pagina terechtkomen")
                            .that(pages.error.isOnErrorPage())
                            .isTrue()
            );
        });
    }

    @Test
    @Order(2)
    @Story("Error heading zichtbaar")
    @Description("Controleert dat de error heading correct wordt weergegeven op de error pagina.")
    @DisplayName("Error heading is correct")
    public void errorHeadingIsVisible() {
        pages.error.navigateTo("invalid-path-123");

        String heading = pages.error.getHeadingText();
        assertTrue(
                heading.contains("Deze pagina is nergens te vinden"),
                "Error heading niet correct. Gevonden: " + heading
        );
    }

    @Test
    @Order(3)
    @Story("Terug naar Home knop zichtbaar")
    @Description("Controleert dat de 'Terug naar Home' knop zichtbaar is op de error pagina.")
    @DisplayName("'Terug naar Home' knop zichtbaar")
    public void backHomeButtonIsVisible() {
        Allure.step("Navigeer naar invalide route", () -> {
            pages.error.navigateTo("page-not-found");
        });

        Allure.step("Controleer zichtbaarheid 'Terug naar Home' knop", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("'Terug naar Home' knop moet zichtbaar zijn")
                            .that(pages.error.isBackHomeButtonVisible())
                            .isTrue()
            );
        });
    }

    @Test
    @Order(4)
    @Story("Terug naar Home navigatie")
    @Description("Controleert dat het klikken op 'Terug naar Home' de gebruiker terug naar de homepage brengt.")
    @DisplayName("'Terug naar Home' navigeert correct")
    public void backHomeButtonNavigatesToHome() {
        Allure.step("Navigeer naar invalide route", () -> {
            pages.error.navigateTo("invalid-route");
        });

        Allure.step("Verifieer dat error pagina wordt getoond", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Moet eerst op error pagina staan")
                            .that(pages.error.isOnErrorPage())
                            .isTrue()
            );
        });

        Allure.step("Klik op 'Terug naar Home'", () -> {
            pages.error.clickBackHomeButton();
        });

        Allure.step("Wacht tot homepage geladen is", () -> {
            WebDriverWait wait = new WebDriverWait(pages.getDriver(), Duration.ofSeconds(10));
            wait.until(driver -> pages.home.isOnHomePage());
        });

        Allure.step("Controleer dat gebruiker terug op homepage is", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Gebruiker moet terug op de homepage zijn")
                            .that(pages.home.isOnHomePage())
                            .isTrue()
            );
        });
    }

    @Test
    @Order(5)
    @Story("Meerdere invalide routes gaan naar error page")
    @Description("Controleert dat verschillende invalide routes allemaal naar de error page leiden.")
    @DisplayName("Meerdere invalide routes tonen error pagina")
    public void multipleInvalidRoutesLeadToErrorPage() {
        String[] invalidRoutes = {
                "completely-fake-page",
                "this/does/not/exist",
                "404-test-12345",
                "nonexistent/subpath"
        };

        for (String route : invalidRoutes) {

            Allure.step("Navigeer naar invalide route: " + route, () -> {
                pages.error.navigateTo(route);
            });

            Allure.step("Controleer dat error pagina wordt getoond voor route: " + route, () -> {
                assertWithScreenshotOnFailure(() ->
                        assertWithMessage("Route '" + route + "' moet naar error pagina leiden")
                                .that(pages.error.isOnErrorPage())
                                .isTrue()
                );
            });

            Allure.step("Ga terug naar homepage voor volgende iteratie", () -> {
                pages.home.navigateTo();
            });
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

