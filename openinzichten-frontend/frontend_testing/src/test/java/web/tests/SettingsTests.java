package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Instellingen pagina")
public class SettingsTests {
    private AllOpenInzichtPages pages;

    private String username;
    private String email;
    private final String password = "Password1!";
    private String uniqueEmail ="story+" + System.currentTimeMillis() + "@mail.com";

    @BeforeEach
    @Step("Setup: gebruiker registreren")
    public void setup() {
        pages = new AllOpenInzichtPages();

        String unique = String.valueOf(System.currentTimeMillis()).substring(6);
        username = "SettingsUser" + unique;
        email = "settings+" + unique + "@mail.com";

        Allure.step("Registreer nieuwe gebruiker", () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(username, email, password, password);
            pages.register.registerStepTwo("3500", "Depressie");
        });

        assertWithScreenshot(() ->
                assertWithMessage("Gebruiker is niet ingelogd na registratie")
                        .that(pages.settings.isUserLoggedIn())
                        .isTrue()
        );
    }

    @Test
    @Order(1)
    @Story("Test: navigeren naar instellingen pagina")
    @Description("Controleert dat een ingelogde gebruiker succesvol naar de instellingen pagina kan navigeren.")
    @DisplayName("Navigeren naar instellingen pagina")
    public void navigateToSettings_pageLoadsSuccessfully() {
        Allure.step("Given gebruiker is ingelogd", this::ensureLoggedIn);

        Allure.step("Navigeer naar instellingen pagina", () -> {
            pages.settings.hoverOverUsername();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            pages.settings.navigateToSettings();
        });

        Allure.step("Controleren dat de instellingen pagina wordt getoond", ()-> {
            assertWithScreenshot(() ->
                assertWithMessage("Instellingen pagina moet succesvol laden")
                        .that(pages.settings.isOnSettingsPage("settings"))
                        .isTrue()
        );
        });
    }

    @Test
    public void updateAllProfileFields_savesSuccessfully() {
        // Log in explicitly to avoid token/401 race conditions
        pages.login.navigateTo();
        pages.login.loginAndWait(uniqueEmail, password, java.time.Duration.ofSeconds(6));
        pages.settings.navigateToSettings();
        pages.settings.clickProfileTab();

        // Capture previous state
        String prevUsername = pages.settings.getUsername();
        String prevZipLabel = pages.settings.getZipcodeDisplayedLabel();
        boolean prevHasCondition = pages.settings.isHasConditionChecked();

        // Change all fields
        String newUsername = prevUsername + "Upd";
        String newEmail = "story+" + System.currentTimeMillis() + "@mail.com";
        pages.settings.setUsername(newUsername);
        pages.settings.setEmail(newEmail);
        pages.settings.selectFirstZipcodeSuggestionDifferentFrom("3990");
        pages.settings.setHasCondition(!prevHasCondition);
        // Ensure at least one condition present: add 'Depressie' if not already selected
        boolean hasDepressie = pages.settings.getSelectedConditionChips().stream()
                .anyMatch(ch -> ch.getText().trim().startsWith("Depressie"));
        if (!hasDepressie) {
            pages.settings.addConditionByName("Depressie");
        }

        // Save and verify toast appears
        pages.settings.clickSaveChanges();
        // Wait for toast message (up to ~8s), accept several common variants
        int toastAttempts = 0;
        while (toastAttempts++ < 32 && !(pages.settings.isToastMessagePresent("Instellingen zijn opgeslagen")
                || pages.settings.isToastMessagePresent("opgeslagen")
                || pages.settings.isToastMessagePresent("Saved")
                || pages.settings.isToastMessagePresent("successfully"))) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        boolean savedToast = pages.settings.isToastMessagePresent("Instellingen zijn opgeslagen")
            || pages.settings.isToastMessagePresent("opgeslagen")
            || pages.settings.isToastMessagePresent("Saved")
            || pages.settings.isToastMessagePresent("successfully");
        assertTrue(savedToast, "Succesbericht voor opslaan ontbreekt.");

        // Verify values updated in UI
        org.junit.jupiter.api.Assertions.assertEquals(newUsername, pages.settings.getUsername(), "Gebruikersnaam niet bijgewerkt.");
        org.junit.jupiter.api.Assertions.assertEquals(newEmail, pages.settings.getEmail(), "E-mailadres niet bijgewerkt.");
        org.junit.jupiter.api.Assertions.assertNotEquals(prevHasCondition, pages.settings.isHasConditionChecked(), "Aandoening checkbox niet bijgewerkt.");
    }

    @Test
    public void updateSingleField_usernameOnly_savesSuccessfully() {
        // Log in explicitly to avoid token/401 race conditions
        pages.login.navigateTo();
        pages.login.loginAndWait(uniqueEmail, password, java.time.Duration.ofSeconds(6));
        pages.settings.navigateToSettings();
        pages.settings.clickProfileTab();

        String prevUsername = pages.settings.getUsername();
        String newUsername = prevUsername + "x";
        pages.settings.setUsername(newUsername);

        pages.settings.clickSaveChanges();
        // Wait for toast message (up to ~8s), accept several common variants
        int toastAttempts2 = 0;
        while (toastAttempts2++ < 32 && !(pages.settings.isToastMessagePresent("Instellingen zijn opgeslagen")
                || pages.settings.isToastMessagePresent("opgeslagen")
                || pages.settings.isToastMessagePresent("Saved")
                || pages.settings.isToastMessagePresent("successfully"))) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        boolean savedToast2 = pages.settings.isToastMessagePresent("Instellingen zijn opgeslagen")
            || pages.settings.isToastMessagePresent("opgeslagen")
            || pages.settings.isToastMessagePresent("Saved")
            || pages.settings.isToastMessagePresent("successfully");
        assertTrue(savedToast2, "Succesbericht voor opslaan ontbreekt.");
        org.junit.jupiter.api.Assertions.assertEquals(newUsername, pages.settings.getUsername(), "Gebruikersnaam niet bijgewerkt.");
    }

    @Test
    public void restoreChanges_returnsFieldsToOriginalSnapshot() {
        // Log in explicitly to avoid token/401 race conditions
        pages.login.navigateTo();
        pages.login.loginAndWait(uniqueEmail, password, java.time.Duration.ofSeconds(6));
        pages.settings.navigateToSettings();
        pages.settings.clickProfileTab();

        // Snapshot current values
        String origUser = pages.settings.getUsername();
        String origEmail = pages.settings.getEmail();
        String origZip = pages.settings.getZipcodeDisplayedLabel();
        boolean origHasCondition = pages.settings.isHasConditionChecked();

        // Make changes to all fields (do not save)
        pages.settings.setUsername(origUser + "_tmp");
        pages.settings.setEmail("story+" + System.currentTimeMillis() + "@mail.com");
        pages.settings.selectFirstZipcodeSuggestionDifferentFrom(origZip);
        pages.settings.setHasCondition(!origHasCondition);

        // Click restore and verify fields match snapshot
        pages.settings.clickRestoreChanges();
        org.junit.jupiter.api.Assertions.assertEquals(origUser, pages.settings.getUsername(), "Herstellen: gebruikersnaam wijkt af.");
        org.junit.jupiter.api.Assertions.assertEquals(origEmail, pages.settings.getEmail(), "Herstellen: e-mailadres wijkt af.");
        org.junit.jupiter.api.Assertions.assertEquals(origZip, pages.settings.getZipcodeDisplayedLabel(), "Herstellen: postcode wijkt af.");
        org.junit.jupiter.api.Assertions.assertEquals(origHasCondition, pages.settings.isHasConditionChecked(), "Herstellen: checkbox wijkt af.");
    }


    @AfterEach
    @Step("Cleanup: account verwijderen en browser sluiten")
    public void tearDown() {
        if (pages != null) {
            try {
                if (!pages.settings.isUserLoggedIn()) {
                    pages.login.navigateTo();
                    pages.login.loginAndWait(email, password, java.time.Duration.ofSeconds(6));
                }

                if (pages.settings.isUserLoggedIn()) {
                    try { pages.settings.navigateToSettings(); pages.settings.deleteAccountAndWait(password, "Je account is succesvol verwijderd"); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            pages.closeBrowser();
        }
    }

    private void ensureLoggedIn() {
        if (!pages.settings.isUserLoggedIn()) {
            pages.login.navigateTo();
            pages.login.loginAndWait(email, password, java.time.Duration.ofSeconds(6));
        }
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
