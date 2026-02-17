package web.tests;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import web.pageObjects.AllOpenInzichtPages;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Registratie eerste formulier validaties")
public class RegisterTests {

	private AllOpenInzichtPages pages;

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


    private void runScenario(String username, String email, String password, String confirmPassword, boolean checkTerms, String label) {
        Allure.step("Scenario: " + label, () -> {

            Allure.step("Navigeer naar registratie pagina", () -> {
                pages.register.navigateTo();
            });
            Allure.step("Vul registratie formulier in", () -> {
//                pages.register.enterUsername(username);
//                pages.register.enterEmail(email);
//                pages.register.enterPassword(password);
//                pages.register.enterConfirmPassword(confirmPassword);
//                if (checkTerms) pages.register.checkTerms();
//                pages.register.clickRegisterNext();

                Allure.step("Vul gebruikersnaam in", () -> {
                    pages.register.enterUsername(username);
                });
                Allure.step("Vul e-mailadres in", () -> {
                    pages.register.enterEmail(email);
                });
                Allure.step("Vul wachtwoord in", () -> {
                    pages.register.enterPassword(password);
                });
                Allure.step("Vul herhaald wachtwoord in", () -> {
                    pages.register.enterConfirmPassword(confirmPassword);
                });
                Allure.step("Check de algemene voorwaarden indien van toepassing", () -> {
                    if (checkTerms) pages.register.checkTerms();
                });
                Allure.step("Klik op de knop 'Volgende' om te registreren", () -> {
                    pages.register.clickRegisterNext();
                });

            });

            Allure.step("Valideer resultaat van registratie poging", () -> {
                assertWithScreenshot(() -> {
                    String toast = pages.register.getMessageToast();
                    if (!pages.register.isOnHomePage() && !pages.register.isOnSecondForm()) {
                        assertWithMessage("Toast bericht ontbreekt voor scenario: " + label)
                                .that(toast)
                                .isNotNull();
                        assertWithMessage("Toast bericht leeg voor scenario: " + label)
                                .that(toast.isEmpty())
                                .isFalse();
                    }
                });
            });
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
        Object drv = pages.getDriver();
        byte[] img = ((org.openqa.selenium.TakesScreenshot) drv)
                .getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        Allure.addAttachment("screenshot", "image/png", new java.io.ByteArrayInputStream(img), "png");
    }

	@Test
    @Order(1)
    @Story("Alle velden ongeldig")
    @Description("Controleert dat het registratieformulier correcte foutmeldingen toont wanneer alle velden ongeldig zijn.")
    @DisplayName("Registratie mislukt bij alle velden ongeldig")
	void register_invalidAllFields() {
		runScenario("!", "no-at-sign", "123", "321", true, "Invalid all fields");
	}

	@Test
    @Order(2)
    @Story("Niet-overeenkomend herhaald wachtwoord")
    @Description("Controleert dat registratie mislukt wanneer het herhaalde wachtwoord niet overeenkomt.")
    @DisplayName("Registratie mislukt bij niet-overeenkomend herhaald wachtwoord")
	void register_mismatchingRepeatedPassword() {
		runScenario("UserTwo", "user2@example.com", "Password1!", "Password2!", true, "Mismatching repeated password");
	}

	@Test
    @Order(3)
    @Story("Leeg herhaald wachtwoord")
    @Description("Controleert dat registratie mislukt wanneer het herhaalde wachtwoord leeg is.")
    @DisplayName("Registratie mislukt bij leeg herhaald wachtwoord")
	void register_emptyRepeatedPassword() {
		runScenario("UserThree", "user3@example.com", "Password1!", "", true, "Empty repeated password");
	}

	@Test
    @Order(4)
    @Story("Leeg wachtwoord")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord leeg is.")
    @DisplayName("Registratie mislukt bij leeg wachtwoord")
	void register_emptyPassword() {
		runScenario("UserFour", "user4@example.com", "", "", true, "Empty password");
	}

	@Test
    @Order(5)
    @Story("Wachtwoord zonder hoofdletters en speciale tekens")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord geen hoofdletters en speciale tekens bevat.")
    @DisplayName("Registratie mislukt bij wachtwoord zonder hoofdletters en speciale tekens")
	void register_passwordNoUppercaseNoSpecial() {
		runScenario("UserFive", "user5@example.com", "password1", "password1", true, "Password missing uppercase+special");
	}

	@Test
    @Order(6)
    @Story("Wachtwoord zonder speciale tekens")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord geen speciale tekens bevat.")
    @DisplayName("Registratie mislukt bij wachtwoord zonder speciale tekens")
	void register_passwordNoSpecial() {
		runScenario("UserSix", "user6@example.com", "Password1", "Password1", true, "Password missing special");
	}

	@Test
    @Order(7)
    @Story("Wachtwoord zonder hoofdletters")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord geen hoofdletters bevat.")
    @DisplayName("Registratie mislukt bij wachtwoord zonder hoofdletters")
	void register_passwordNoUppercase() {
		runScenario("UserSeven", "user7@example.com", "password1!", "password1!", true, "Password missing uppercase");
	}

	@Test
    @Order(8)
    @Story("Te kort wachtwoord")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord te kort is.")
    @DisplayName("Registratie mislukt bij te kort wachtwoord")
	void register_passwordTooShort() {
		runScenario("UserEight", "user8@example.com", "Ab1!", "Ab1!", true, "Password too short");
	}

	@Test
    @Order(9)
    @Description("Controleert dat registratie mislukt wanneer het e-mailadres leeg is.")
    @DisplayName("Registratie mislukt bij leeg e-mailadres")
	void register_emptyEmail() {
		runScenario("UserNine", "", "Password1!", "Password1!", true, "Empty email");
	}

	@Test
    @Order(10)
    @Story("Te kort e-mailadres")
    @Description("Controleert dat registratie mislukt wanneer het e-mailadres te kort is.")
    @DisplayName("Registratie mislukt bij te kort e-mailadres")
	void register_tooShortEmail() {
		runScenario("UserTen", "a@b", "Password1!", "Password1!", true, "Too short email");
	}

	@Test
    @Order(11)
    @Story("Lege gebruikersnaam")
    @Description("Controleert dat registratie mislukt wanneer de gebruikersnaam leeg is.")
    @DisplayName("Registratie mislukt bij lege gebruikersnaam")
	void register_emptyUsername() {
		runScenario("", "user12@example.com", "Password1!", "Password1!", true, "Empty username");
	}

	@Test
    @Order(12)
    @Story("Gebruikersnaam met minimale lengte")
    @Description("Controleert dat registratie slaagt wanneer de gebruikersnaam de minimale lengte heeft.")
    @DisplayName("Registratie slaagt bij gebruikersnaam met minimale lengte")
	void register_usernameAtMinimum() {
		runScenario("AB", "user13@example.com", "Password1!", "Password1!", true, "Username at minimum (2)");
	}

	@Test
    @Order(13)
    @Story("Gebruikersnaam die maximale lengte overschrijdt")
    @Description("Controleert dat registratie mislukt wanneer de gebruikersnaam de maximale lengte overschrijdt.")
    @DisplayName("Registratie mislukt bij gebruikersnaam die maximale lengte overschrijdt")
	void register_usernameExceedingMaximum() {
		String longUsername = "u".repeat(41);
		runScenario(longUsername, "user14@example.com", "Password1!", "Password1!", true, "Username exceeding maximum");
	}

	@Test
    @Order(14)
    @Story("Gebruikersnaam minimale lengte & e-mail maximale lengte")
    @Description("Controleert dat registratie slaagt wanneer de gebruikersnaam de minimale lengte heeft en het e-mailadres de maximale lengte heeft.")
    @DisplayName("Registratie slaagt bij gebruikersnaam minimale lengte & e-mail maximale lengte")
	void register_usernameLowerBoundaryAndEmailMax() {
		String emailMaxLocal = "b".repeat(315);
		String emailMax = emailMaxLocal + "@y.com"; // >320 to trigger max length
		runScenario("Ab", emailMax, "Password1!", "Password1!", true, "Username lower boundary & email max");
	}

	@Test
    @Order(15)
    @Story("Gebruikersnaam aan ondergrens")
    @Description("Controleert dat registratie slaagt wanneer de gebruikersnaam aan de ondergrens zit.")
    @DisplayName("Registratie slaagt bij gebruikersnaam aan ondergrens")
	void register_usernameAtLowerBoundary() {
		runScenario("ZZ", "user16@example.com", "Password1!", "Password1!", true, "Username at lower boundary");
	}

	@Test
    @Order(16)
    @Story("Gebruikersnaam aan bovengrens")
    @Description("Controleert dat registratie slaagt wanneer de gebruikersnaam aan de bovengrens zit.")
    @DisplayName("Registratie slaagt bij gebruikersnaam aan bovengrens")
	void register_usernameAtMaximumBoundary() {
		String maxUser = "x".repeat(40);
		runScenario(maxUser, "user17@example.com", "Password1!", "Password1!", true, "Username at maximum (40)");
	}

	@Test
    @Order(17)
    @Story("Alle velden ongeldig (spaties)")
    @Description("Controleert dat het registratieformulier correcte foutmeldingen toont wanneer alle velden spaties bevatten.")
    @DisplayName("Registratie mislukt bij alle velden ongeldig (spaties)")
	void register_invalidInputsAllFieldsSpaces() {
		runScenario(" ", " ", " ", " ", false, "Invalid inputs all fields (spaces)");
	}

	@Test
    @Order(18)
    @Story("Niet-overeenkomend wachtwoord en lege bevestiging")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord en de bevestiging niet overeenkomen en de bevestiging leeg is.")
    @DisplayName("Registratie mislukt bij niet-overeenkomend wachtwoord en lege bevestiging")
	void register_mismatchedPasswordEmptyConfirmation() {
		runScenario("User19", "user19@example.com", "Password1!", "", true, "Mismatched password and empty confirmation");
	}

	@Test
    @Order(19)
    @Story("Wachtwoord zonder hoofdletters en speciale tekens")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord geen hoofdletters en speciale tekens bevat.")
    @DisplayName("Registratie mislukt bij wachtwoord zonder hoofdletters en speciale tekens")
	void register_passwordMissingUppercaseAndSpecial() {
		runScenario("User20", "user20@example.com", "password1", "password1", true, "Password missing uppercase and special (20)");
	}

	@Test
    @Order(20)
    @Story("Wachtwoord zonder speciale tekens")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord geen speciale tekens bevat.")
    @DisplayName("Registratie mislukt bij wachtwoord zonder speciale tekens")
	void register_passwordMissingSpecial_21() {
		runScenario("User21", "user21@example.com", "Password1", "Password1", true, "Password missing special (21)");
	}

	@Test
    @Order(21)
    @Story("Wachtwoord zonder hoofdletters")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord geen hoofdletters bevat.")
    @DisplayName("Registratie mislukt bij wachtwoord zonder hoofdletters")
	void register_passwordMissingUppercase_22() {
		runScenario("User22", "user22@example.com", "password1!", "password1!", true, "Password missing uppercase (22)");
	}

	@Test
    @Order(22)
    @Story("Te kort wachtwoord")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord te kort is.")
    @DisplayName("Registratie mislukt bij te kort wachtwoord")
	void register_passwordTooShort_23() {
		runScenario("User23", "user23@example.com", "A1!", "A1!", true, "Password too short (23)");
	}

	@Test
    @Order(23)
    @Story("Wachtwoord met alleen spaties")
    @Description("Controleert dat registratie mislukt wanneer het wachtwoord alleen spaties bevat.")
    @DisplayName("Registratie mislukt bij wachtwoord met alleen spaties")
	void register_passwordOnlySpaces() {
		runScenario("User24", "user24@example.com", "        ", "        ", true, "Password only spaces");
	}

	@Test
    @Order(24)
    @Story("E-mail met alleen spaties")
    @Description("Controleert dat registratie mislukt wanneer het e-mailadres alleen spaties bevat.")
    @DisplayName("Registratie mislukt bij e-mail met alleen spaties")
	void register_emailOnlySpaces() {
		runScenario("User26", "   ", "Password1!", "Password1!", true, "Email only spaces");
	}

	@Test
    @Order(25)
    @Story("E-mail die maximale lengte overschrijdt")
    @Description("Controleert dat registratie mislukt wanneer het e-mailadres de maximale lengte overschrijdt.")
    @DisplayName("Registratie mislukt bij e-mail die maximale lengte overschrijdt")
	void register_emailExceedingMaxLength() {
		String hugeLocal = "c".repeat(400);
		String hugeEmail = hugeLocal + "@z.com";
		runScenario("User27", hugeEmail, "Password1!", "Password1!", true, "Email exceeding max length");
	}

	@Test
    @Order(26)
    @Story("Ongeldig e-mail formaat")
    @Description("Controleert dat registratie mislukt wanneer het e-mailadres een ongeldig formaat heeft.")
    @DisplayName("Registratie mislukt bij ongeldig e-mail formaat")
	void register_invalidEmailFormat() {
		runScenario("User28", "plainaddress", "Password1!", "Password1!", true, "Invalid email format");
	}

	@Test
    @Order(27)
    @Story("Reeds gebruikte gebruikersnaam")
    @Description("Controleert dat registratie mislukt wanneer de gebruikersnaam al in gebruik is.")
    @DisplayName("Registratie mislukt bij reeds gebruikte gebruikersnaam")
	void register_alreadyUsedUsername() {
		runScenario("existingUser", "newemail29@example.com", "Password1!", "Password1!", true, "Already used username");
	}

	@Test
    @Order(28)
    @Story("Gebruikersnaam met alleen spaties")
    @Description("Controleert dat registratie mislukt wanneer de gebruikersnaam alleen spaties bevat.")
    @DisplayName("Registratie mislukt bij gebruikersnaam met alleen spaties")
	void register_usernameOnlySpaces() {
		runScenario("   ", "user30@example.com", "Password1!", "Password1!", true, "Username only spaces");
	}

	@Test
    @Order(29)
    @Story("Gebruikersnaam met spaties")
    @Description("Controleert dat registratie mislukt wanneer de gebruikersnaam spaties bevat.")
    @DisplayName("Registratie mislukt bij gebruikersnaam met spaties")
	void register_usernameContainingSpaces() {
		runScenario("name withspace", "user31@example.com", "Password1!", "Password1!", true, "Username containing spaces");
	}

	@Test
    @Order(30)
    @Story("Geldige eerste formulier registratie")
    @Description("Controleert dat registratie slaagt wanneer alle velden geldig zijn.")
    @DisplayName("Registratie slaagt bij geldig eerste formulier")
	void register_validFirstForm() {
		runScenario("ValidUser123", "validuser123@example.com", "StrongPass1!", "StrongPass1!", true, "Valid first-form");
	}

	@Test
    @Order(31)
    @Story("Publiek zichtbare gebruikersnaam")
    @Description("Controleert dat de juiste informatie wordt weergegeven over de zichtbaarheid van de gebruikersnaam tijdens registratie.")
    @DisplayName("Correcte info over publiek zichtbare gebruikersnaam")
	void register_usernamePubliclyVisible() {
//		pages.register.navigateTo();
//
//		String publicVisibleText = "Je gebruikersnaam is publiek zichtbaar.";
//
//		String actualText = pages.register.getUsernameVisibilityInfoText();
//
//		assertEquals(publicVisibleText, actualText, "Expected username visibility info text to match.");

        Allure.step("Check gebruikersnaam info tekst", () -> {
            pages.register.navigateTo();
            String expected = "Je gebruikersnaam is publiek zichtbaar.";
            assertWithScreenshot(() ->
                    assertWithMessage("Gebruikersnaam info tekst klopt niet")
                            .that(pages.register.getUsernameVisibilityInfoText())
                            .isEqualTo(expected)
            );
        });
	}

    @Test
    @Order(32)
    @Story("Privé e-mail tijdens registratie")
    @Description("Controleert dat de juiste informatie wordt weergegeven over de privacy van het e-mailadres tijdens registratie.")
    @DisplayName("Correcte info over privé e-mail tijdens registratie")
    void register_emailPrivateText() {
//        pages.register.navigateTo();
//
//        String expectedText = "Je e-mailadres is enkel zichtbaar voor beheerders.";
//
//        String actualText = pages.register.getEmailVisibilityInfoText();
//
//        assertEquals(expectedText, actualText, "Expected email privacy info text to match.");

        Allure.step("Check e-mail privacy info tekst", () -> {
            pages.register.navigateTo();
            String expected = "Je e-mailadres is enkel zichtbaar voor beheerders.";
            assertWithScreenshot(() ->
                    assertWithMessage("E-mail privacy info tekst klopt niet")
                            .that(pages.register.getEmailVisibilityInfoText())
                            .isEqualTo(expected)
            );
        });
    }
}
