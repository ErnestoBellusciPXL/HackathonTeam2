package web.tests;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import web.pageObjects.AllOpenInzichtPages;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Feature("About pagina navigatie")
public class AboutUsTests {
	private AllOpenInzichtPages pages;

	@BeforeEach
	public void setup() {
		pages = new AllOpenInzichtPages();
		pages.home.navigateTo();
	}

	@AfterEach
	public void teardown() {
		if (pages != null) {
			pages.closeBrowser();
		}
	}

	@Test
	@Story("Navigatie via navbar")
	@Description("Controleert dat de About-pagina bereikbaar is via de navigatiebalk.")
	public void navigateToAboutPageViaNavigationBar() {
		pages.about.navigateViaNavigationBar();

		assertTrue(
				pages.about.isOnAboutPage(),
				"Navigatie via navbar faalde. Gevonden titel: " + pages.about.getHeadingText()
		);
	}

	@Test
	@Story("Navigatie via footer")
	@Description("Controleert dat de About-pagina bereikbaar is via de footer-link.")
	public void navigateToAboutPageViaFooterLink() {
		pages.about.scrollDownToFooterLink();
		pages.about.navigateViaFooterLink();

		assertTrue(
				pages.about.isOnAboutPage(),
				"Navigatie via footer faalde. Gevonden titel: " + pages.about.getHeadingText()
		);
	}
}
