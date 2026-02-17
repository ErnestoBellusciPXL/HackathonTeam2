package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import web.pageObjects.AllOpenInzichtPages;

import java.time.Duration;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Chat Functionaliteit")
public class ChatTests {
	private AllOpenInzichtPages pages;
	private String unique;
	private String user1;
	private String email1;
	private String user2;
	private String email2;

	@BeforeEach
    @Step("Setup testomgeving")
	public void setup() {
		pages = new AllOpenInzichtPages();
		// Use a fixed value so names/emails/titles are the same each run
		unique = "fixedTest";
	}

	@AfterEach
    @Step("Cleanup: verwijder testaccounts en sluit browser")
	public void teardown() {
        try {
            if (email1 != null) {
                Allure.step("Verwijder account user1", () -> {
                    pages.login.navigateTo();
                    pages.login.login(email1, "Password1!");
                    pages.settings.hoverOverUsername();
                    pages.settings.navigateToSettings();
                    pages.settings.deleteAccount("Password1!");
                });
            }
        } catch (Exception ignored) {}
        try {
            if (email2 != null) {
                Allure.step("Verwijder account user2", () -> {
                    pages.login.navigateTo();
                    pages.login.login(email2, "Password1!");
                    pages.settings.hoverOverUsername();
                    pages.settings.navigateToSettings();
                    pages.settings.deleteAccount("Password1!");
                });
            }
        } catch (Exception ignored) {}
        pages.closeBrowser();
	}

	private WebDriver getDriverFromChatLauncher() {
		try {
			java.lang.reflect.Field f = pages.chatLauncher.getClass().getSuperclass().getDeclaredField("driver");
			f.setAccessible(true);
			return (WebDriver) f.get(pages.chatLauncher);
		} catch (Exception e) {
			throw new RuntimeException("Kon WebDriver niet ophalen uit chatLauncher: " + e.getMessage(), e);
		}
	}

	private void typeAndSendUsingChatSendButton(String message) {
		WebDriver driver = getDriverFromChatLauncher();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));

		By inputSel = By.cssSelector("input[placeholder='Typ een bericht...']");
		WebElement input = wait.until(ExpectedConditions.elementToBeClickable(inputSel));
		// Set value via JS and dispatch input event so Vue v-model updates
		((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));", input, message);

		// Wait until the input's value equals the message we set (ensures v-model updated)
		wait.until(ExpectedConditions.attributeToBe(inputSel, "value", message));

		By sendBtnSel = By.id("sendChatButton");
		WebElement sendBtn = wait.until(ExpectedConditions.elementToBeClickable(sendBtnSel));
		sendBtn.click();

		// wait for the message to appear
		By msgLoc = By.xpath("//div[contains(@class,'text-sm') and contains(normalize-space(.), '" + message + "')]");
		wait.until(ExpectedConditions.visibilityOfElementLocated(msgLoc));
	}

	private void typeAndSendAndWaitForToast(String message, String expectedToast) {
		WebDriver driver = getDriverFromChatLauncher();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));

		By inputSel = By.cssSelector("input[placeholder='Typ een bericht...']");
		WebElement input = wait.until(ExpectedConditions.elementToBeClickable(inputSel));
		((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));", input, message);
		wait.until(ExpectedConditions.attributeToBe(inputSel, "value", message));

		By sendBtnSel = By.id("sendChatButton");
		WebElement sendBtn = wait.until(ExpectedConditions.elementToBeClickable(sendBtnSel));
		sendBtn.click();

		By toastSel = By.id("message-toasts");
		wait.until(ExpectedConditions.visibilityOfElementLocated(toastSel));
		wait.until(ExpectedConditions.textToBePresentInElementLocated(toastSel, expectedToast));

        try{
            assertWithMessage("De verwachte foutmelding werd niet getoond.")
                    .that(driver.findElement(toastSel).getText())
                    .contains(expectedToast);
        } catch(AssertionError e){
            takeScreenShot();
            throw e;
        }
	}

    private void registerUser(String username, String email) throws InterruptedException {
        Allure.step("Registreer gebruiker: " + username, () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(username, email, "Password1!", "Password1!");
            pages.register.registerStepTwo("3500", "Depressie");
            Thread.sleep(1200);
        });
    }

    private void login(String email) throws InterruptedException {
        Allure.step("Log in als: " + email, () -> {
            pages.login.navigateTo();
            boolean ok = pages.login.loginAndWait(email, "Password1!", Duration.ofSeconds(6));
            try{
                assertWithMessage("Login mislukt voor gebruiker: " + email)
                        .that(ok).isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    private void logout() {
        Allure.step("Uitloggen", () -> {
            pages.settings.hoverOverUsername();
            pages.settings.navigateToSettings();
            pages.settings.clickLogout();
        });
    }

    private void createStory(String title, String content) throws InterruptedException {
        Allure.step("Maak nieuw verhaal: " + title, () -> {
            pages.writeStory.hoverOverUsername();
            Thread.sleep(500);
            pages.writeStory.navigateToWriteStory();
            pages.writeStory.writeStory(title, content);
        });
    }

    private void sendConnectionRequestForStory(String storyTitle) throws InterruptedException {
        Allure.step("Stuur connectieverzoek via verhaal: " + storyTitle, () -> {
            pages.storyView.openStoryByTitle(storyTitle);
            Thread.sleep(800);
            pages.storyDetail.clickMakeConnection();
        });
    }

    private void acceptConnectionRequest(String requesterUsername) {
        Allure.step("Accepteer connectieverzoek van: " + requesterUsername, () -> {
            pages.connections.navigateToRequestsViaNav();
            boolean accepted = pages.connections.acceptRequestForUser(requesterUsername);
            try{
                assertWithMessage("Connectieverzoek van " + requesterUsername + " niet gevonden of niet geaccepteerd")
                        .that(accepted).isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    private void openChatWith(String username) throws InterruptedException {
        Allure.step("Open chat met gebruiker: " + username, () -> {
            pages.connections.navigateToConnectionsTab();
            Thread.sleep(500);
            pages.connections.openChatForUser(username);
        });
    }

    private void sendChatMessage(String msg) {
        Allure.step("Verzend chatbericht: " + (msg.length() > 30 ? msg.substring(0,30)+"..." : msg), () -> {
            typeAndSendUsingChatSendButton(msg);
        });
    }

    private void assertMessageVisible(String msg) {
        Allure.step("Controleer dat chatbericht zichtbaar is: " + msg, () -> {
            boolean visible = pages.chatLauncher.isMessageVisibleInOpenChat(msg);
            try {
                assertWithMessage("Bericht niet zichtbaar in chat: " + msg)
                        .that(visible).isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    public void takeScreenShot(){
        Object drv = pages.getDriver();
        byte[] img = ((org.openqa.selenium.TakesScreenshot) drv).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        io.qameta.allure.Allure.addAttachment("screenshot", "image/png", new java.io.ByteArrayInputStream(img), "png");
    }

	@Test
    @Order(1)
    @Story("Chat tussen twee verbonden gebruikers")
    @Description("Controleert dat berichten kunnen worden uitgewisseld tussen twee verbonden gebruikers")
    @DisplayName("Testen van chat functionaliteit tussen twee verbonden gebruikers")
	public void chat_between_two_connected_users() throws InterruptedException {
        user1 = "ChatUserA" + unique;
        email1 = "chata+" + unique + "@mail.com";
        registerUser(user1, email1);
        String storyTitle = "ChatStory" + unique;
        createStory(storyTitle, "Inhoud voor chat testcase zodat deze user connecties kan maken.");
        logout();

        // Register user2 and send connection request
        user2 = "ChatUserB" + unique;
        email2 = "chatb+" + unique + "@mail.com";
        registerUser(user2, email2);
        sendConnectionRequestForStory(storyTitle);
        logout();

        // user1 logs in and accepts
        login(email1);
        acceptConnectionRequest(user2);

        // In connections, open chat for user2 and send a message using the chat send button
        openChatWith(user2);
        String msg1 = "Hallo van " + user1;
        sendChatMessage(msg1);
        logout();

        // login user2 and verify message arrived via ChatLauncher
        login(email2);
        pages.chatLauncher.openLauncher();
        pages.chatLauncher.openOverview();
        pages.chatLauncher.openChatByUsername(user1);
        assertMessageVisible(msg1);

        // reply from user2 and verify user1 sees it after login. Use send button again.
        String reply = "Hoi terug van " + user2;
        sendChatMessage(reply);
        logout();

        // login user1 to verify reply
        login(email1);
        // open chat from connections
        openChatWith(user2);
        assertMessageVisible(reply);
	}

	@Test
    @Order(2)
    @Story("Lang bericht in chat tussen twee verbonden gebruikers")
    @Description("Controleert dat het systeem een foutmelding geeft bij het verzenden van een te lang bericht in de chat tussen twee verbonden gebruikers")
    @DisplayName("Testen van foutmelding bij te lang chatbericht tussen twee verbonden gebruikers")
	public void chat_long_message_between_two_connected_users() throws InterruptedException {
        // Register user1 and create story
        user1 = "ChatLongUserA" + unique;
        email1 = "chatlonga+" + unique + "@mail.com";
        registerUser(user1, email1);
        String storyTitle = "ChatLongStory" + unique;
        createStory(storyTitle, "Een kort verhaal om de connectie te kunnen maken.");
        logout();

        // Register user2 and send connection request
        user2 = "ChatLongUserB" + unique;
        email2 = "chatlongb+" + unique + "@mail.com";
        registerUser(user2, email2);
        sendConnectionRequestForStory(storyTitle);
        logout();

        // user1 logs in and accepts
        login(email1);
        acceptConnectionRequest(user2);

        // In connections, open chat for user2 and send a very long message (1K+ characters)
        openChatWith(user2);
        String longMsg = "x".repeat(1001);
        // attempt to send a too-long message and assert the toast appears with the expected text
        String expectedToast = "Bericht mag maximaal 1000 tekens bevatten.";

        Allure.step("Controleer dat de foutmelding werd getoond", () -> {
            typeAndSendAndWaitForToast(longMsg, expectedToast);
        });
        // If the toast with the expected message appeared, the test is successful
	}

    @Test
    @Order(3)
    @Story("Chat bericht maximaal toegestaan")
    @Description("Controleert dat berichten van maximale lengte (1000 tekens) correct verzonden worden")
    @DisplayName("Testen van chatbericht van maximale lengte tussen twee verbonden gebruikers")
    public void chat_long_message_below_maximum_between_two_connected_users() throws InterruptedException {
        // Register user1 and create story
        user1 = "ChatLongUserA" + unique;
        email1 = "chatlonga+" + unique + "@mail.com";
        registerUser(user1, email1);
        String storyTitle = "ChatLongStory" + unique;
        createStory(storyTitle, "Een kort verhaal om de connectie te kunnen maken.");
        logout();

        // Register user2 and send connection request
        user2 = "ChatLongUserB" + unique;
        email2 = "chatlongb+" + unique + "@mail.com";
        registerUser(user2, email2);
        sendConnectionRequestForStory(storyTitle);
        logout();

        // user1 logs in and accepts
        login(email1);
        acceptConnectionRequest(user2);

        // In connections, open chat for user2 and send a very long message below maximum (999 characters)
        openChatWith(user2);
        String longMsg = "x".repeat(999);
        sendChatMessage(longMsg);
        assertMessageVisible(longMsg);
    }

    @Test
    @Order(4)
    @Story("Chat bericht maximaal toegestaan")
    @Description("Controleert dat berichten van maximale lengte (1000 tekens) correct verzonden worden")
    @DisplayName("Testen van chatbericht van maximale lengte tussen twee verbonden gebruikers")
    public void chat_long_message_equal_to_maximum_between_two_connected_users() throws InterruptedException {
        // Register user1 and create story
        user1 = "ChatLongUserA" + unique;
        email1 = "chatlonga+" + unique + "@mail.com";
        registerUser(user1, email1);
        String storyTitle = "ChatLongStory" + unique;
        createStory(storyTitle, "Een kort verhaal om de connectie te kunnen maken.");
        logout();

        // Register user2 and send connection request
        user2 = "ChatLongUserB" + unique;
        email2 = "chatlongb+" + unique + "@mail.com";
        registerUser(user2, email2);
        sendConnectionRequestForStory(storyTitle);
        logout();

        // user1 logs in and accepts
        login(email1);
        acceptConnectionRequest(user2);

        // In connections, open chat for user2 and send a very long message below maximum (999 characters)
        openChatWith(user2);
        String longMsg = "x".repeat(1000);
        sendChatMessage(longMsg);
        assertMessageVisible(longMsg);
    }

	@Test
    @Order(5)
    @Story("Chat bericht met alleen spaties")
    @Description("Controleert dat het systeem een foutmelding geeft bij het verzenden van een bericht dat alleen uit spaties bestaat in de chat tussen twee verbonden gebruikers")
    @DisplayName("Testen van foutmelding bij chatbericht met alleen spaties tussen twee verbonden gebruikers")
	public void chat_spaces_only_message_shows_error() throws InterruptedException {
        // Register user1 and create story
        user1 = "ChatLongUserA" + unique;
        email1 = "chatlonga+" + unique + "@mail.com";
        registerUser(user1, email1);
        String storyTitle = "ChatLongStory" + unique;
        createStory(storyTitle, "Een kort verhaal om de connectie te kunnen maken.");
        logout();

        // Register user2 and send connection request
        user2 = "ChatLongUserB" + unique;
        email2 = "chatlongb+" + unique + "@mail.com";
        registerUser(user2, email2);
        sendConnectionRequestForStory(storyTitle);
        logout();

        // user1 logs in and accepts
        login(email1);
        acceptConnectionRequest(user2);

        // In connections, open chat for user2 and send a message met alleen spaties
        openChatWith(user2);
        String spacesOnlyMsg = "     "; // 5 spaties
        String expectedToast = "Bericht mag niet alleen uit spaties bestaan.";
        // If the toast with the expected message appeared, the test is successful

        Allure.step("Controleer dat de foutmelding werd getoond", () -> {
            typeAndSendAndWaitForToast(spacesOnlyMsg, expectedToast);
        });
	}

    @Test
    @Order(6)
    @Story("Chat leeg bericht")
    @Description("Controleert dat het systeem een foutmelding geeft bij het verzenden van een leeg bericht in de chat tussen twee verbonden gebruikers")
    @DisplayName("Testen van foutmelding bij leeg chatbericht tussen twee verbonden gebruikers")
    public void chat_empty_message_shows_error() throws InterruptedException {
        // Register user1 and create story
        user1 = "ChatLongUserA" + unique;
        email1 = "chatlonga+" + unique + "@mail.com";
        registerUser(user1, email1);
        String storyTitle = "ChatLongStory" + unique;
        createStory(storyTitle, "Een kort verhaal om de connectie te kunnen maken.");
        logout();

        // Register user2 and send connection request
        user2 = "ChatLongUserB" + unique;
        email2 = "chatlongb+" + unique + "@mail.com";
        registerUser(user2, email2);
        sendConnectionRequestForStory(storyTitle);
        logout();

        // user1 logs in and accepts
        login(email1);
        acceptConnectionRequest(user2);

        // In connections, open chat for user2 and send a message met alleen spaties
        openChatWith(user2);
        String emptyMsg = ""; // 5 spaties
        String expectedToast = "Bericht mag niet alleen uit spaties bestaan.";

        Allure.step("Controleer dat de foutmelding werd getoond", () -> {
            typeAndSendAndWaitForToast(emptyMsg, expectedToast);
        });
    }


    @Test
    @Order(7)
    @Story("Chat overzicht openen via chat icoon")
    @Description("Controleert dat het chat overzicht wordt geopend bij het klikken op het chat icoon")
    @DisplayName("Testen van openen chat overzicht via chat icoon")
    public void is_chat_overview_open_when_clicking_chaticon() throws InterruptedException {
        // User1: register and create a story
        user1 = "ConnUserA" + unique;
        email1 = "connA+" + unique + "@mail.com";
        registerUser(user1, email1);
        String storyTitle = "VerhaalDoor" + unique;
        createStory(storyTitle, "Inhoud van het verhaal zodat dit lang genoeg is voor de testcase.");
        logout();

        // User2: register, find the story and send connect request
        user2 = "ConnUserB" + unique;
        email2 = "connB+" + unique + "@mail.com";
        registerUser(user2, email2);
        sendConnectionRequestForStory(storyTitle);
        logout();

        // User1 logs in and accepts the request
        login(email1);
        acceptConnectionRequest(user2);
        // Navigate to connections page
        pages.chatLauncher.openLauncher();
        boolean chatOverviewVisible = pages.chatLauncher.isChatsOverviewVisible();

        Allure.step("valideer dat chat overzicht wordt geopend", () -> {
            try{
                assertWithMessage("Expected chat overview to be visible after clicking chat icon.")
                        .that(chatOverviewVisible).isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }
}
