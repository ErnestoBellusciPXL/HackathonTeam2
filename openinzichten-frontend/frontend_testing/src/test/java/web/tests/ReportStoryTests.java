package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Verhaal rapporteren")
public class ReportStoryTests {
    private AllOpenInzichtPages pages;
    private String toastMessage;
    private String validPassword = "Password1!";
    private String validPostalCode = "3500";
    private String validCondition = "Depressie";
    private String unique = "fixedTest";
    private String user1 = "RepUserA" + unique;
    private String email1 = "repA+" + unique + "@mail.com";
    private String user2 = "RepUserB" + unique;
    private String email2 = "repB+" + unique + "@mail.com";
    private String storyTitle = "Verhaal Door " + user1;
    private String succesMessage = "Bedankt, je melding is ontvangen.";
    private String errorMessage = "Beschrijf kort waarom je dit verhaal wilt rapporteren";
    private String errorExceedingMinimum = "Beschrijf kort waarom je dit verhaal wilt rapporteren (minimaal 5 tekens).";
    private String errorExceedingMaximum = "De omschrijving mag maximaal 250 tekens bevatten.";

    @BeforeEach
    @Step("Setup: gebruikers registreren en verhaal aanmaken")
    public void setup(){
        pages = new AllOpenInzichtPages();
        registerUser(user1, email1);
        writeStoryAsUser1();
        registerUser(user2, email2);
    }

    @AfterEach
    public void tearDown(){
        // Try to delete both created accounts (ignore any errors)
        try {
            if (email1 != null) {
                try {
                    deleteAccount(email1, validPassword);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        try {
            if (email2 != null) {
                try {
                    deleteAccount(email2, validPassword);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        if (pages != null) {
            pages.closeBrowser();
        }
    }

    @Test
    @Order(1)
    @Story("Geldig rapporteren van verhaal")
    @Description("Testcase voor het succesvol rapporteren van een verhaal met een geldige reden.")
    @DisplayName("Testen van geldig rapporteren van verhaal")
    public void testValidReportStory(){
        reportAndAssert("This is a valid report reason.", succesMessage);
    }

    @Test
    @Order(2)
    @Story("Geldig rapporteren van verhaal met minder dan maximum posities")
    @Description("Testcase voor het succesvol rapporteren van een verhaal met een geldige reden die minder dan het maximum aantal posities bevat.")
    @DisplayName("Testen van geldig rapporteren van verhaal met minder dan maximum posities")
    public void testValidReportReasonWithLessThanMaximumPositions(){
        reportAndAssert(repeat('R', 249), succesMessage);
    }

    @Test
    @Order(3)
    @Story("Ongeldig rapporteren van verhaal met lege reden")
    @Description("Testcase voor het ongeldig rapporteren van een verhaal met een lege reden.")
    @DisplayName("Testen van ongeldig rapporteren van verhaal met lege reden")
    public void testInvalidReportReasonEmpty(){
        reportAndAssert("", errorMessage);
    }

    @Test
    @Order(4)
    @Story("Ongeldig rapporteren van verhaal met alleen spaties als reden")
    @Description("Testcase voor het ongeldig rapporteren van een verhaal met alleen spaties als reden.")
    @DisplayName("Testen van ongeldig rapporteren van verhaal met alleen spaties als reden")
    public void testInvalidReportReasonOnlySpaces(){
        reportAndAssert("     ", errorMessage);
    }

    @Test
    @Order(5)
    @Story("Ongeldig rapporteren van verhaal met reden die maximum posities overschrijdt")
    @Description("Testcase voor het ongeldig rapporteren van een verhaal met een reden die het maximum aantal posities overschrijdt.")
    @DisplayName("Testen van ongeldig rapporteren van verhaal met reden die maximum posities overschrijdt")
    public void testInvalidReportReasonExceedsMaximumPositions(){
        reportAndAssert(repeat('R', 251), errorExceedingMaximum);
    }

    @Test
    @Order(6)
    @Story("Geldig rapporteren van verhaal met reden die gelijk is aan maximum posities")
    @Description("Testcase voor het succesvol rapporteren van een verhaal met een reden die gelijk is aan het maximum aantal posities.")
    @DisplayName("Testen van geldig rapporteren van verhaal met reden die gelijk is aan maximum posities")
    public void testValidReportReasonEqualsMaximum(){
        reportAndAssert(repeat('R', 250), succesMessage);
    }

    @Test
    @Order(7)
    @Story("Ongeldig rapporteren van verhaal met reden die minder is dan minimum posities")
    @Description("Testcase voor het ongeldig rapporteren van een verhaal met een reden die minder is dan het minimum aantal posities.")
    @DisplayName("Testen van ongeldig rapporteren van verhaal met reden die minder is dan minimum posities")
    public void testInvalidReportReasonExceedsMinimumPositions(){
        reportAndAssert(repeat('R', 4), errorExceedingMinimum);
    }

    @Test
    @Order(8)
    @Story("Geldig rapporteren van verhaal met reden die gelijk is aan minimum posities")
    @Description("Testcase voor het succesvol rapporteren van een verhaal met een reden die gelijk is aan het minimum aantal posities.")
    @DisplayName("Testen van geldig rapporteren van verhaal met reden die gelijk is aan minimum posities")
    public void testValidReportReasonEqualsMinimum(){
        reportAndAssert(repeat('R', 5), succesMessage);
    }

    @Test
    @Order(9)
    @Story("Geldig rapporteren van verhaal met reden die minimum posities met één overschrijdt")
    @Description("Testcase voor het succesvol rapporteren van een verhaal met een reden die het minimum aantal posities met één overschrijdt.")
    @DisplayName("Testen van geldig rapporteren van verhaal met reden die minimum posities met één overschrijdt")
    public void testValidReportReasonExceedsMinimumByOne(){
        reportAndAssert(repeat('R', 6), succesMessage);
    }


    private void registerUser(String username, String email){
        Allure.step("Registreer user" + username, () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(username, email, validPassword, validPassword);
            pages.register.registerStepTwo(validPostalCode, validCondition);
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            pages.settings.logout();
        });
    }

    private void writeStoryAsUser1(){
        Allure.step("User1 schrijft een verhaal", () -> {
            // User1: login and write story
            pages.login.navigateTo();
            pages.login.login(email1, validPassword);
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
            pages.writeStory.hoverOverUsername();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            pages.writeStory.navigateToWriteStory();
            pages.writeStory.writeStory(storyTitle, "Inhoud van het verhaal zodat dit lang genoeg is voor de testcase.");
            // logout user1
            pages.settings.logout();
        });
    }

    private void loginUser2(){
        Allure.step("User2 logt in", () -> {
            pages.login.navigateTo();
            pages.login.login(email2, validPassword);
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        });
    }

    private void deleteAccount(String email, String password) throws InterruptedException {
        Allure.step("Verwijder account voor " + email, () -> {
            pages.login.navigateTo();
            pages.login.login(email, password);
            Thread.sleep(800);
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

    private void reportAndAssert(String reason, String expectedMessage) {
        Allure.step("Given gebruiker 2 is ingelogd", this::loginUser2);

        Allure.step("When verhaal wordt gerapporteerd", () -> {
            pages.storyView.openStoryByTitle(storyTitle);
            pages.storyDetail.clickReportStory();
            pages.storyDetail.clickReportReason();
            pages.storyDetail.selectOption("OTHER");
            pages.storyDetail.enterOtherReason(reason);
            pages.storyDetail.clickSendReport();
        });

        Allure.step("Then correcte toast wordt getoond", () -> {
            assertWithScreenshot(() ->
                    assertWithMessage("Verkeerde toastmelding")
                            .that(pages.storyDetail.getMessageToast())
                            .contains(expectedMessage)
            );
        });
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
