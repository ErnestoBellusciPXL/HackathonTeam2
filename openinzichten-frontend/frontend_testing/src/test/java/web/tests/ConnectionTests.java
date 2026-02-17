package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Connectie Functionaliteit")
public class ConnectionTests {

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
        unique = "fixedTest";
    }

    @AfterEach
    @Step("Cleanup: verwijder testaccounts en sluit browser")
    public void teardown() {
        deleteAccountIfExists(email1);
        deleteAccountIfExists(email2);
        pages.closeBrowser();
    }

    private void deleteAccountIfExists(String email) {
        if (email == null) return;
        try {
            pages.login.navigateTo();
            pages.login.login(email, "Password1!");
            Thread.sleep(800);
            pages.settings.hoverOverUsername();
            pages.settings.navigateToSettings();
            pages.settings.deleteAccount("Password1!");
        } catch (Exception ignored) {}
    }

    private void registerUser(String username, String email) throws InterruptedException {
        Allure.step("Registreer gebruiker: " + username, () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(username, email, "Password1!", "Password1!");
            pages.register.registerStepTwo("3500", "Depressie");
        });
        Thread.sleep(1500);
    }

    private void loginUser(String email) throws InterruptedException {
        Allure.step("Log in met email: " + email, () -> {
            pages.login.navigateTo();
            pages.login.login(email, "Password1!");
        });
        Thread.sleep(1000);
    }

    private void logout() {
        Allure.step("Log uit", () -> {
            pages.settings.hoverOverUsername();
            pages.settings.navigateToSettings();
            pages.settings.clickLogout();
        });
    }

    private void writeStory(String title, String content) throws InterruptedException {
        Allure.step("Ga naar 'Verhaal schrijven' via hover", () -> {
            pages.writeStory.hoverOverUsername();
        });
        Thread.sleep(500);

        Allure.step("Maak nieuw verhaal aan", () -> {
            pages.writeStory.navigateToWriteStory();
            pages.writeStory.writeStory(title, content);
        });
    }

    private void openStoryFromList(String title) throws InterruptedException {
        Allure.step("Open verhaal met titel: " + title, () -> {
            pages.storyView.openStoryByTitle(title);
        });
        Thread.sleep(1000);
    }

    private void sendConnectionRequest() throws InterruptedException {
        Allure.step("Verzend connectieverzoek", () -> {
            pages.storyDetail.clickMakeConnection();
        });
        Thread.sleep(500);
    }

    private void acceptRequest(String username) {
        Allure.step("Accepteer connectieverzoek van: " + username, () -> {
            pages.connections.navigateToRequestsViaNav();
            boolean accepted = pages.connections.acceptRequestForUser(username);

            try{
                assertWithMessage("Kon verzoek van '" + username + "' niet accepteren.")
                        .that(accepted)
                        .isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    private void declineRequest(String username) {
        Allure.step("Weiger connectieverzoek van: " + username, () -> {
            pages.connections.navigateToRequestsViaNav();
            boolean declined = pages.connections.declineRequestForUser(username);

            try {
                assertWithMessage("Kon verzoek van '" + username + "' niet weigeren.")
                        .that(declined)
                        .isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    private void verifyUserInConnections(String username) {
        Allure.step("Valideer aanwezigheid van '" + username + "' in connectielijst", () -> {
            pages.connections.navigateToConnectionsTab();
            boolean exists = pages.connections.isUserInConnections(username);

            try{
                assertWithMessage("Gebruiker '" + username + "' zou in de connecties moeten staan.")
                        .that(exists)
                        .isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    private void verifyUserNotInConnections(String username) {
        Allure.step("Valideer dat '" + username + "' niet in connectielijst staat", () -> {
            pages.connections.navigateToConnectionsTab();
            boolean exists = pages.connections.isUserInConnections(username);

            try{
                assertWithMessage("Gebruiker '" + username + "' zou NIET in de connecties mogen staan.")
                        .that(exists)
                        .isFalse();
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
    @Story("Connectieverzoek accepteren")
    @Description("Controleert dat een gebruiker een connectieverzoek kan accepteren en dat de verbinding correct wordt weergegeven.")
    @DisplayName("Testen van het accepteren van een connectieverzoek")
    public void connection_flow_accept() throws Exception {
        user1 = "ConnUserA" + unique;
        email1 = "connA+" + unique + "@mail.com";
        registerUser(user1, email1);

        String story = "Story" + unique;
        writeStory(story, "Inhoud van het verhaal voor de testcase.");
        logout();

        user2 = "ConnUserB" + unique;
        email2 = "connB+" + unique + "@mail.com";
        registerUser(user2, email2);
        openStoryFromList(story);
        sendConnectionRequest();
        logout();

        loginUser(email1);
        acceptRequest(user2);
        verifyUserInConnections(user2);
    }

    @Test
    @Order(2)
    @Story("Connectieverzoek weigeren")
    @Description("Controleert dat een gebruiker een connectieverzoek kan weigeren en dat de verbinding niet wordt weergegeven.")
    @DisplayName("Testen van het weigeren van een connectieverzoek")
    public void connection_flow_decline() throws Exception {
        user1 = "ConnUserC" + unique;
        email1 = "connC+" + unique + "@mail.com";
        registerUser(user1, email1);

        String story = "DeclineStory" + unique;
        writeStory(story, "Verhaalinhoud voor decline testcase.");
        logout();

        user2 = "ConnUserD" + unique;
        email2 = "connD+" + unique + "@mail.com";
        registerUser(user2, email2);
        openStoryFromList(story);
        sendConnectionRequest();
        logout();

        loginUser(email1);
        declineRequest(user2);
        verifyUserNotInConnections(user2);
    }

    @Test
    @Order(3)
    @Story("Connectie verwijderen")
    @Description("Controleert dat een gebruiker een bestaande connectie kan verwijderen en dat deze niet langer wordt weergegeven.")
    @DisplayName("Testen van het verwijderen van een connectie")
    public void connection_flow_delete_connection() throws Exception {
        user1 = "ConnUserA" + unique;
        email1 = "connA+" + unique + "@mail.com";
        registerUser(user1, email1);

        String story = "DeleteStory" + unique;
        writeStory(story, "Testinhoud voor delete testcase.");
        logout();

        user2 = "ConnUserB" + unique;
        email2 = "connB+" + unique + "@mail.com";
        registerUser(user2, email2);
        openStoryFromList(story);
        sendConnectionRequest();
        logout();

        loginUser(email1);
        acceptRequest(user2);

        Allure.step("Verwijder connectie met " + user2, () -> {
            pages.connections.navigateToConnectionsTab();
            pages.connections.clickDeleteConnection();
        });

        Thread.sleep(500);
        verifyUserNotInConnections(user2);
    }

    @Test
    @Order(4)
    @Story("Navigeren naar connectiespagina")
    @Description("Controleert dat een gebruiker succesvol naar de connectiespagina kan navigeren.")
    @DisplayName("Testen van navigatie naar connectiespagina")
    public void navigate_to_connections_page() throws Exception {
        user1 = "ConnUserA" + unique;
        email1 = "connA+" + unique + "@mail.com";
        registerUser(user1, email1);

        Allure.step("Navigeer naar connectiespagina", () -> {
            pages.connections.navigateToConnectionsTab();

            try{
                assertWithMessage("Had verwacht op de verbindingenpagina te staan.")
                        .that(pages.connections.isOnConnectionsPage("connections"))
                        .isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }

    @Test
    @Order(5)
    @Story("Chat openen met connectie")
    @Description("Controleert dat een gebruiker een chatvenster kan openen met een bestaande connectie.")
    @DisplayName("Testen van het openen van een chat met een connectie")
    public void chat_open_with_connection() throws Exception {
        user1 = "ConnUserA" + unique;
        email1 = "connA+" + unique + "@mail.com";
        registerUser(user1, email1);

        String story = "ChatStory" + unique;
        writeStory(story, "Dit is een geldige verhaal voor de test.");
        logout();

        user2 = "ConnUserB" + unique;
        email2 = "connB+" + unique + "@mail.com";
        registerUser(user2, email2);
        openStoryFromList(story);
        sendConnectionRequest();
        logout();

        loginUser(email1);
        acceptRequest(user2);

        Allure.step("Open chat met " + user2, () -> {
            pages.connections.navigateToConnectionsTab();
            pages.connections.openChatForUser(user2);

            try{
                assertWithMessage("Chatvenster met '" + user2 + "' zou open moeten zijn.")
                        .that(pages.connections.isChatOpenWithConnection())
                        .isTrue();
            } catch(AssertionError e){
                takeScreenShot();
                throw e;
            }
        });
    }
}

