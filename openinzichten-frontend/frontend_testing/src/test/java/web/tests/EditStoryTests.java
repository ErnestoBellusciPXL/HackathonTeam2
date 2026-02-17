package web.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertWithMessage;
import io.qameta.allure.Description;
import java.time.Duration;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Verhalen beheren")
public class EditStoryTests {
    private AllOpenInzichtPages pages;
    private String toast;
    private String newStoryTitle = "Mijn Bijgewerkte Verhaal";
    private String newStoryContent = "Dit is een bijgewerkt verhaal met voldoende lengte.";
    private String succesMessageUpdate  = "Je verhaal is gewijzigd.";
    private String succesMessageDelete = "Je verhaal is verwijderd.";
    private String uniqueUser;
    private String uniqueEmail;
    private final String password = "Password1!";

    @BeforeEach
    @Description("Registreer gebruiker en maak initieel verhaal aan")
    public void setup() {
        pages = new AllOpenInzichtPages();

        String unique = String.valueOf(System.currentTimeMillis());
        uniqueUser = "StoryUser" + unique;
        uniqueEmail = "story+" + unique + "@mail.com";

        Allure.step("Registreer testgebruiker", () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(uniqueUser, uniqueEmail, password, password);
            pages.register.registerStepTwo("3500", "Depressie");
        });

        Allure.step("Maak initieel verhaal aan", () -> {
            sleep(1000);
            pages.writeStory.hoverOverUsername();
            sleep(1000);
            pages.writeStory.navigateToWriteStory();
            pages.writeStory.enterTitle("Mijn Eerste Verhaal");
            pages.writeStory.enterStory("Geldige inhoud, dit verhaal moet lang genoeg zijn");
            pages.writeStory.clickSubmit();
        });
    }

    @AfterEach
    @Description("Cleanup: verwijder account indien nog aanwezig en sluit browser")
    public void tearDown() {
        if (pages != null) {
            Allure.step("Verwijder testaccount indien nog aanwezig", () -> {
                try {
                    pages.login.navigateTo();
                    boolean loggedIn = pages.login.loginAndWait(
                            uniqueEmail,
                            password,
                            Duration.ofSeconds(6)
                    );

                    if (loggedIn) {
                        try {
                            pages.settings.deleteAccountAndWait(password, succesMessageDelete);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            });

            pages.closeBrowser();
        }
    }

    @Test
    @Order(1)
    @Story("Verhaal bewerken")
    @Description("Controleert dat een gebruiker zijn eigen verhaal succesvol kan bewerken")
    @DisplayName("Verhaal bewerken slaagt")
    public void navigateToMyStory_pageLoadsSuccessfully() throws InterruptedException {
        Allure.step("Navigeer naar bewerken van mijn verhaal", this::navigateToEditMyStory);

        Allure.step("Werk verhaal bij", () -> {
            pages.writeStory.writeStory(newStoryTitle, newStoryContent);
        });

        Allure.step("Controleer succesmelding", () -> {
            String toast = pages.writeStory.getMessageToast();

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Toast moet aangeven dat het verhaal is gewijzigd")
                            .that(toast)
                            .contains(succesMessageUpdate)
            );
        });

        Allure.step("Open bijgewerkt verhaal en valideer inhoud", () -> {
            pages.storyView.openStoryByTitle(newStoryTitle);
            sleep(800);

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Titel moet overeenkomen")
                            .that(pages.storyDetail.getStoryTitle())
                            .isEqualTo(newStoryTitle)
            );

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Inhoud moet overeenkomen")
                            .that(pages.storyDetail.getStoryContent())
                            .isEqualTo(newStoryContent)
            );
        });
    }

    @Test
    @Order(2)
    @Story("Verhaal verwijderen")
    @Description("Controleert dat een gebruiker zijn eigen verhaal kan verwijderen")
    @DisplayName("Verhaal verwijderen slaagt")
    public void deleteStory_storyIsRemoved() throws InterruptedException {
        Allure.step("Navigeer naar mijn verhaal", this::navigateToEditMyStory);

        Allure.step("Verwijder verhaal", () -> {
            pages.myStory.scrollDown();
            pages.myStory.clickDeleteStoryButton();
            pages.myStory.clickConfirmDeleteButton();
        });

        Allure.step("Controleer succesmelding", () -> {
            String toast = pages.writeStory.getMessageToast();

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("Toast moet aangeven dat het verhaal is verwijderd")
                            .that(pages.settings.isToastMessagePresent(toast))
                            .isTrue()
            );
        });

        Allure.step("Controleer dat My Story pagina niet meer toegankelijk is", () -> {
            pages.myStory.hoverOverUsername();
            pages.myStory.navigateToMyStory();

            assertWithScreenshotOnFailure(() ->
                    assertWithMessage("My Story pagina mag niet meer toegankelijk zijn na verwijderen")
                            .that(pages.writeStory.isOnMyStoryPage())
                            .isFalse()
            );
        });
    }

    private void navigateToEditMyStory(){
        pages.myStory.hoverOverUsername();
        pages.myStory.navigateToMyStory();
        pages.myStory.clickEditStoryButton();
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
