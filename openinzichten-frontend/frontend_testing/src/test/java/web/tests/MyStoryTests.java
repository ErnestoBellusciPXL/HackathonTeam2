package web.tests;

import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Mijn Verhaal pagina")
public class MyStoryTests {
    private AllOpenInzichtPages pages;
    private final String password = "Password1!";


    @BeforeEach
    @Description("Registreer gebruiker en maak een verhaal aan")
    public void setup() {
        pages = new AllOpenInzichtPages();

        Allure.step("Registreer testgebruiker", () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(
                    "StoryUser",
                    "story@mail.com",
                    password,
                    password
            );
            pages.register.registerStepTwo("3500", "Depressie");
        });

        Allure.step("Navigeer naar schrijf-verhaal pagina", () -> {
            sleep(1000);
            pages.writeStory.hoverOverUsername();
            sleep(1000);
            pages.writeStory.navigateToWriteStory();
        });

        Allure.step("Maak een nieuw verhaal aan", () -> {
            pages.writeStory.enterTitle("Mijn Eerste Verhaal");
            pages.writeStory.enterStory("Geldige inhoud, dit verhaal moet lang genoeg zijn");
            pages.writeStory.clickSubmit();
        });
    }

    @Test
    @Order(1)
    @Story("Navigatie naar Mijn Verhaal")
    @Description("Controleert dat de Mijn Verhaal pagina correct laadt")
    @DisplayName("Mijn Verhaal pagina laden")
    public void navigateToMyStory_pageLoadsSuccessfully() {
        Allure.step("Navigeer naar Mijn Verhaal pagina", () -> {
            pages.myStory.hoverOverUsername();
            pages.myStory.navigateToMyStory();
        });

        Allure.step("Valideer dat Mijn Verhaal pagina correct geladen is", () -> {
            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Mijn Verhaal pagina moet correct laden")
                            .that(pages.myStory.isOnMyStoryPage("my-story"))
                            .isTrue()
            );
        });
    }

    @AfterEach
    @Description("Verwijder testaccount en sluit browser")
    public void tearDown() {
        try {
            Allure.step("Verwijder testaccount", () -> {
                pages.settings.hoverOverUsername();
                pages.settings.navigateToSettings();
                pages.settings.deleteAccount(password);
            });
        } catch (Exception ignored) {}

        if (pages != null) {
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }
}
