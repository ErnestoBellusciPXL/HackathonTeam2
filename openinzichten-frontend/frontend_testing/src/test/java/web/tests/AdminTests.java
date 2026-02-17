package web.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import web.pageObjects.AllOpenInzichtPages;
import java.time.Duration;

import static com.google.common.truth.Truth.assertWithMessage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Feature("Admin Functionaliteit")
public class AdminTests {
    private AllOpenInzichtPages pages;
    private String unique;
    private String authorUser;
    private String authorEmail;
    private final String authorPassword = "Password1!";
    private String reporterUser;
    private String reporterEmail;
    private final String reporterPassword = "Password1!";
    private String storyTitle;

    @BeforeEach
    @Step("Setup testdata: accounts, verhaal en rapportages aanmaken")
    public void setup() throws InterruptedException {
        pages = new AllOpenInzichtPages();

        Allure.step("Genereer unieke identifiers voor author en reporter", () -> {
            unique = String.valueOf(System.currentTimeMillis());
            authorUser = "AuthorA" + unique;
            authorEmail = "authorA+" + unique + "@mail.com";
            reporterUser = "ReporterA" + unique;
            reporterEmail = "reporterA+" + unique + "@mail.com";
            storyTitle = "Test Verhaal " + unique;
        });

        Allure.step("Author account registreren en verhaal schrijven", () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(authorUser, authorEmail, authorPassword, authorPassword);
            pages.register.registerStepTwo("3500", "Depressie");
            Thread.sleep(1200);

            pages.login.navigateTo();
            pages.login.login(authorEmail, authorPassword);
            Thread.sleep(800);
            pages.writeStory.navigateTo();
            pages.writeStory.writeStory(storyTitle, "Inhoud van het testverhaal voor admin rapportage.");
            pages.settings.logout();
            Thread.sleep(800);
        });

        Allure.step("Reporter account registreren en verhaal rapporteren", () -> {
            pages.register.navigateTo();
            pages.register.registerStepOne(reporterUser, reporterEmail, reporterPassword, reporterPassword);
            pages.register.registerStepTwo("3500", "Depressie");
            Thread.sleep(1200);

            pages.login.navigateTo();
            pages.login.login(reporterEmail, reporterPassword);
            pages.storyView.navigateTo();
            Thread.sleep(800);
            pages.storyView.openStoryByTitle(storyTitle);
            Thread.sleep(800);
            pages.storyDetail.clickReportStory();
            pages.storyDetail.clickReportReason();
            pages.storyDetail.selectOption("OTHER");
            pages.storyDetail.enterOtherReason("Reported by second user for admin tests.");
            pages.storyDetail.clickSendReport();
            Thread.sleep(1200);
            pages.settings.logout();
            Thread.sleep(800);
        });

        Allure.step("Admin login en rapport tab openen", () -> {
            pages.login.navigateTo();
            boolean loggedInAdmin = pages.login.loginAndWait("admin@openinzicht.be", "0penInzicht?1PXL", Duration.ofSeconds(8));
            if (!loggedInAdmin) throw new RuntimeException("Admin login failed during setup");

            pages.admin.ensureOnAdmin();
            pages.admin.openReportTab();

            if (!pages.admin.isTableVisible()) {
                pages.login.navigateTo();
                pages.login.loginAndWait("admin@openinzicht.be", "0penInzicht?1PXL", Duration.ofSeconds(8));
                pages.admin.navigateTo();
                pages.admin.openReportTab();
                Thread.sleep(800);
            }
        });
    }


    @Test
    @Order(1)
    @Story("Filteren op open tickets")
    @Description("Controleert dat alleen open tickets zichtbaar zijn na filteren.")
    @DisplayName("Testen van filteren op open tickets in admin tabel")
    public void testFilterByOpenStatus() {
        Allure.step("Controleer dat admin tabel zichtbaar is", () ->{
                    try {
                        assertWithMessage("Admin tickets tabel was niet zichtbaar bij filter 'open'")
                                .that(pages.admin.isTableVisible())
                                .isTrue();
                    } catch (AssertionError e) {
                        takeScreenShot();
                        throw e;
                    }
                }
        );

        Allure.step("Klik op 'open' filter", () ->
                pages.admin.clickStatusFilter("open")
        );

        Allure.step("Valideer dat alle tickets open zijn of geen tickets aanwezig", () ->{
                    try {
                        assertWithMessage("Niet alle tickets hebben status 'open' of de tabel bevat tickets terwijl dit niet verwacht werd")
                                .that(pages.admin.hasTicketsWithStatus("open") || pages.admin.getTicketCount() == 0)
                                .isTrue();
                    } catch (AssertionError e) {
                        takeScreenShot();
                        throw e;
                    }
                }
        );
    }

    @Test
    @Order(2)
    @Story("Filteren op gesloten tickets")
    @Description("Controleert dat alleen gesloten tickets zichtbaar zijn na filteren.")
    @DisplayName("Testen van filteren op gesloten tickets in admin tabel")
    public void testFilterByClosedStatus() {
        Allure.step("Controleer dat admin tabel zichtbaar is", () -> {
                    try {
                        assertWithMessage("Admin tickets tabel was niet zichtbaar bij filter 'gesloten'")
                                .that(pages.admin.isTableVisible())
                                .isTrue();
                    } catch (AssertionError e) {
                        takeScreenShot();
                        throw e;
                    }
                }
        );

        Allure.step("Klik op 'gesloten' filter", () ->
                pages.admin.clickStatusFilter("gesloten")
        );

        Allure.step("Valideer dat alle tickets gesloten zijn of geen tickets aanwezig", () -> {
            try {
                assertWithMessage("Niet alle tickets hebben status 'gesloten' of de tabel bevat tickets terwijl dit niet verwacht werd")
                        .that(pages.admin.hasTicketsWithStatus("gesloten") || pages.admin.getTicketCount() == 0)
                        .isTrue();
            } catch (AssertionError e) {
                takeScreenShot();
                throw e;
            }
        }
        );
    }

    @Test
    @Order(3)
    @Story("Filteren op alle tickets")
    @Description("Controleert dat de tabel zichtbaar blijft bij filter 'alle'.")
    @DisplayName("Testen van filteren op alle tickets in admin tabel")
    public void testFilterByAllStatus() {
        Allure.step("Controleer dat admin tabel zichtbaar is", () -> {
                    try {
                        assertWithMessage("Admin tickets tabel was niet zichtbaar bij filter 'alle'")
                                .that(pages.admin.isTableVisible())
                                .isTrue();
                    } catch (AssertionError e) {
                        takeScreenShot();
                        throw e;
                    }
                }
        );

        Allure.step("Klik op 'alle' filter", () ->
                pages.admin.clickStatusFilter("alle")
        );

        Allure.step("Controleer dat tabel nog steeds zichtbaar is", () -> {
                    try {
                        assertWithMessage("Admin tickets tabel verdween bij filter 'alle'")
                                .that(pages.admin.isTableVisible())
                                .isTrue();
                    } catch (AssertionError e) {
                        takeScreenShot();
                        throw e;
                    }
                }
        );
    }

    @Test
    @Order(4)
    @Story("Controleren kolommen in gerapporteerde verhalen tabel")
    @Description("Controleert dat alleen de vereiste kolommen zichtbaar zijn en correct benoemd.")
    @DisplayName("Testen van zichtbare kolommen in admin gerapporteerde verhalen tabel")
    public void testReportedStoriesTableShowsOnlyRequiredColumns() {
        Allure.step("Controleer dat admin tabel zichtbaar is", () -> {
                    try {
                        assertWithMessage("Admin tickets tabel was niet zichtbaar bij controle kolommen")
                                .that(pages.admin.isTableVisible())
                                .isTrue();
                    } catch (AssertionError e) {
                        takeScreenShot();
                        throw e;
                    }
        });

        Allure.step("Lees kolomkoppen", () -> {
            String dateHeader = pages.admin.getTableHeaderText(0);
            String reportedUserHeader = pages.admin.getTableHeaderText(1);
            String storyHeader = pages.admin.getTableHeaderText(2);
            String reasonHeader = pages.admin.getTableHeaderText(3);
            String statusHeader = pages.admin.getTableHeaderText(4);

            Allure.step("Valideer kolomkoppen", () -> {
                try{
                    assertWithMessage("Kolom 0 moet 'Datum' zijn").that(dateHeader).contains("Datum");
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
                }
                try{
                    assertWithMessage("Kolom 1 moet 'Gerapporteerde' zijn").that(reportedUserHeader).contains("Gerapporteerde");
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
                }
                try{
                    assertWithMessage("Kolom 2 moet 'Verhaal' zijn").that(storyHeader).contains("Verhaal");
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
                }
                try{
                    assertWithMessage("Kolom 3 moet 'Reden' zijn").that(reasonHeader).contains("Reden");
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
                }
                try{
                    assertWithMessage("Kolom 4 moet 'Status' zijn").that(statusHeader).contains("Status");
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
                }
            });
        });

        Allure.step("Controleer dat alleen vereiste kolommen zichtbaar zijn", () -> {
                try{
                    assertWithMessage("Er zijn extra kolommen zichtbaar die niet getoond mogen worden")
                            .that(pages.admin.areOnlyRequiredColumnsVisible())
                            .isTrue();
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
                }
        });
    }

    @Test
    @Order(5)
    @Story("Goedgekeurd gerapporteerd verhaal")
    @Description("Controleert dat een admin een gerapporteerd verhaal kan goedkeuren en dat status verandert naar 'Gesloten'.")
    @DisplayName("Testen van goedkeuren van gerapporteerd verhaal door admin")
    public void testApproveReportedStory() throws InterruptedException {
        Allure.step("Controleer dat admin tabel zichtbaar is", () -> {
            try{
                    assertWithMessage("Admin tickets tabel was niet zichtbaar bij goedkeuren van verhaal")
                            .that(pages.admin.isTableVisible())
                            .isTrue();
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
            }
        });

        if (pages.admin.getBekijkenButtonCount() == 0) return;

        Allure.step("Klik op eerste 'Bekijken' knop", () ->
                pages.admin.clickBekijkenButton(0)
        );

        Allure.step("Valideer detailpagina en zichtbaarheid actieknoppen", () -> {
            try {
                assertWithMessage("Niet op ticket detailpagina na klikken op 'Bekijken'").that(pages.admin.isOnTicketDetailPage()).isTrue();
            } catch (AssertionError e) {
                takeScreenShot();
                throw e;
            }
            try {
                assertWithMessage("Actieknoppen niet zichtbaar op ticket detailpagina").that(pages.admin.isActionButtonsVisible()).isTrue();
            } catch (AssertionError e) {
                takeScreenShot();
                throw e;
            }
        });

        Allure.step("Klik op 'Goedkeuren'", () ->
                pages.admin.clickApproveButton()
        );

        Allure.step("Wacht tot status update", () ->
                Thread.sleep(2000)
        );

        Allure.step("Controleer dat status 'Gesloten' is", () -> {
                try {
                    assertWithMessage("Status van verhaal is niet 'Gesloten' na goedkeuren")
                            .that(pages.admin.getCurrentStatus())
                            .isEqualTo("Gesloten");
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
                }
        });
    }

    @Test
    @Order(6)
    @Story("Verwijderen gerapporteerd verhaal")
    @Description("Controleert dat een admin een gerapporteerd verhaal kan verwijderen en dat status verandert naar 'Gesloten'.")
    @DisplayName("Testen van verwijderen van gerapporteerd verhaal door admin")
    public void testDeleteReportedStory() throws InterruptedException {
        Allure.step("Controleer dat admin tabel zichtbaar is", () -> {
                try{
                    assertWithMessage("Admin tickets tabel was niet zichtbaar bij verwijderen van verhaal")
                            .that(pages.admin.isTableVisible())
                            .isTrue();
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
                }
        });

        if (pages.admin.getBekijkenButtonCount() == 0) return;

        Allure.step("Klik op eerste 'Bekijken' knop", () ->
                pages.admin.clickBekijkenButton(0)
        );

        Allure.step("Valideer detailpagina en zichtbaarheid actieknoppen", () -> {
            try{
                assertWithMessage("Niet op ticket detailpagina na klikken op 'Bekijken'").that(pages.admin.isOnTicketDetailPage()).isTrue();
            } catch (AssertionError e) {
                takeScreenShot();
                throw e;
            }
            try{
                assertWithMessage("Actieknoppen niet zichtbaar op ticket detailpagina").that(pages.admin.isActionButtonsVisible()).isTrue();
            } catch (AssertionError e) {
                takeScreenShot();
                throw e;
            }
        });

        Allure.step("Klik op 'Verwijderen'", () ->
                pages.admin.clickDeleteButton()
        );

        Allure.step("Wacht tot status update", () ->
                Thread.sleep(2000)
        );

        Allure.step("Controleer dat status 'Gesloten' is", () -> {
                try {
                    assertWithMessage("Status van verhaal is niet 'Gesloten' na verwijderen")
                            .that(pages.admin.getCurrentStatus())
                            .isEqualTo("Gesloten");
                } catch (AssertionError e) {
                    takeScreenShot();
                    throw e;
                }
        });
    }

    @AfterEach
    @Step("Verwijder testaccounts en sluit browser")
    public void tearDown() {
        try {
            if (authorEmail != null) {
                Allure.step("Verwijder author account", () -> {
                    pages.login.navigateTo();
                    pages.login.login(authorEmail, authorPassword);
                    Thread.sleep(800);
                    pages.settings.hoverOverUsername();
                    pages.settings.navigateToSettings();
                    pages.settings.deleteAccount(authorPassword);
                    Thread.sleep(800);
                });
            }
        } catch (Exception ignored) {}

        try {
            if (reporterEmail != null) {
                Allure.step("Verwijder reporter account", () -> {
                    pages.login.navigateTo();
                    pages.login.login(reporterEmail, reporterPassword);
                    Thread.sleep(800);
                    pages.settings.hoverOverUsername();
                    pages.settings.navigateToSettings();
                    pages.settings.deleteAccount(reporterPassword);
                    Thread.sleep(800);
                });
            }
        } catch (Exception ignored) {}

        try { Allure.step("Log out admin en sluit browser", () -> pages.settings.logout()); } catch (Exception ignored) {}
        pages.closeBrowser();
    }


    public void takeScreenShot(){
        Object drv = pages.getDriver();
        byte[] img = ((org.openqa.selenium.TakesScreenshot) drv).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        io.qameta.allure.Allure.addAttachment("screenshot", "image/png", new java.io.ByteArrayInputStream(img), "png");
    }
}
