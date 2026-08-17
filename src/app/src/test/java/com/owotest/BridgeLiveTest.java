package com.owotest;

import com.owo.controller.AuthController;
import com.owo.controller.RefundController;
import com.owo.dao.PemesananDAO;
import com.owo.dao.RefundDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.Refund;
import com.owo.entity.PemesananStatus;
import com.owo.utils.BridgeInstaller;
import com.owo.utils.Json;
import com.owo.utils.JavaScriptBridge;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real shell in a real {@link WebView}.
 *
 * <p>Whether a bridge method is callable from JavaScript cannot be established by calling
 * it from Java, and whether a screen renders cannot be established by reading its markup.
 * This is the only layer that answers either question.
 *
 * <p>Requires a display, so it is opt-in: run with {@code -Dowo.live=true}. The reflective
 * contract in {@link JavaScriptBridgeContractTest} runs unconditionally and is what CI
 * relies on.
 */
@EnabledIfSystemProperty(named = "owo.live", matches = "true")
class BridgeLiveTest {

    private static final long TIMEOUT_SECONDS = 20;

    private static boolean toolkitStarted;

    private TempDatabase db;
    private WebEngine engine;
    private JavaScriptBridge bridge;
    private Stage stage;

    @BeforeAll
    static void startToolkit() throws Exception {
        if (toolkitStarted) {
            return;
        }
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        assertTrue(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        // Closing the last window would otherwise shut the toolkit down, so every test
        // after the first would fail to create a stage.
        Platform.runLater(() -> Platform.setImplicitExit(false));
        toolkitStarted = true;
    }

    @AfterAll
    static void stopToolkit() {
        Platform.exit();
    }

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
        bridge = new JavaScriptBridge();

        CountDownLatch loaded = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        runOnFxThread(() -> {
            WebView view = new WebView();
            engine = view.getEngine();

            stage = new Stage();
            stage.setScene(new Scene(view, 1200, 800));

            engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                if (state == Worker.State.SUCCEEDED) {
                    // The same wiring the application uses, so the test cannot pass
                    // against a setup the real App never performs.
                    BridgeInstaller.install(engine, bridge);
                    loaded.countDown();
                } else if (state == Worker.State.FAILED) {
                    failure.set(engine.getLoadWorker().getException());
                    loaded.countDown();
                }
            });

            engine.load(getClass().getResource("/com/owo/boundary/App.html").toExternalForm());
            stage.show();
        });

        assertTrue(loaded.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "App.html did not load");
        assertNotNull(engine);
        if (failure.get() != null) {
            throw new AssertionError("App.html failed to load", failure.get());
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // Each test opens a window; leaving them open exhausts the window manager and
        // makes a later test's setUp time out rather than the test itself failing.
        if (stage != null) {
            runOnFxThread(stage::close);
            stage = null;
        }
        if (bridge != null) {
            BridgeInstaller.shutdown(bridge);
        }
        // db.close() stops the notification poller: these tests sign in and never sign
        // out, and one left running polls the database that close() deletes.
        db.close();
    }

    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "FX action did not complete");
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
    }

    /** Evaluates an expression on the FX thread and returns its value. */
    private Object eval(String script) throws Exception {
        AtomicReference<Object> result = new AtomicReference<>();
        runOnFxThread(() -> result.set(engine.executeScript(script)));
        return result.get();
    }

    /** Waits until {@code condition} evaluates truthy, polling the page. */
    private void await(String condition) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000;
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(eval("!!(" + condition + ")"))) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for: " + condition);
    }

    @Test
    void theShellRendersTheLoginScreen() throws Exception {
        await("window.App && document.getElementById('loginForm')");

        assertEquals(Boolean.TRUE, eval("!!document.getElementById('email')"));
        assertEquals(Boolean.TRUE, eval("!!document.getElementById('password')"));
    }

    @Test
    void everyScreenFragmentRendersWithoutError() throws Exception {
        await("window.App");

        String[] screens = {"LoginForm", "RegisterForm"};
        for (String screen : screens) {
            runOnFxThread(() -> engine.executeScript("window.App.navigate('" + screen + "')"));
            await("document.getElementById('app').innerHTML.length > 100");
        }
    }

    @Test
    void registeringThenNavigatingReachesTheBookingScreen() throws Exception {
        await("window.App && document.getElementById('registerForm') "
                + "|| document.getElementById('loginForm')");

        runOnFxThread(() -> engine.executeScript("window.App.navigate('RegisterForm')"));
        await("document.getElementById('registerForm')");

        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('name').value = 'Budi';"
                        + "document.getElementById('email').value = 'budi@example.com';"
                        + "document.getElementById('password').value = 'password123';"
                        + "document.getElementById('registerForm')"
                        + "  .dispatchEvent(new Event('submit', {cancelable: true}));"));

        // Registration must open a session and move the user on to booking.
        await("window.App.session && window.App.session.email === 'budi@example.com'");
        await("document.getElementById('flightForm')");
    }

    @Test
    void loggingInWithASeededAccountReachesTheBookingScreen() throws Exception {
        Fixtures.customer();
        // Fixtures mints sequential addresses; register a known one instead.
        com.owo.controller.AuthController.register("Siti", "siti@example.com", "password123");

        await("window.App && document.getElementById('loginForm')");

        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('email').value = 'siti@example.com';"
                        + "document.getElementById('password').value = 'password123';"
                        + "document.getElementById('loginForm')"
                        + "  .dispatchEvent(new Event('submit', {cancelable: true}));"));

        await("window.App.session && window.App.session.nama === 'Siti'");
        await("document.getElementById('flightForm')");
    }

    @Test
    void aWrongPasswordIsReportedAndDoesNotOpenASession() throws Exception {
        com.owo.controller.AuthController.register("Andi", "andi@example.com", "password123");

        await("window.App && document.getElementById('loginForm')");

        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('email').value = 'andi@example.com';"
                        + "document.getElementById('password').value = 'wrong-password';"
                        + "document.getElementById('loginForm')"
                        + "  .dispatchEvent(new Event('submit', {cancelable: true}));"));

        await("document.getElementById('screenError') "
                + "&& !document.getElementById('screenError').classList.contains('hidden')");

        assertEquals(Boolean.FALSE, eval("!!window.App.session"));
        assertEquals(Boolean.TRUE, eval("!!document.getElementById('loginForm')"));
    }

    @Test
    void searchingFlightsShowsRealResults() throws Exception {
        com.owo.controller.AuthController.register("Dewi", "dewi@example.com", "password123");
        Fixtures.flight(3);
        Fixtures.flight(4);

        await("window.App && document.getElementById('loginForm')");
        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('email').value = 'dewi@example.com';"
                        + "document.getElementById('password').value = 'password123';"
                        + "document.getElementById('loginForm')"
                        + "  .dispatchEvent(new Event('submit', {cancelable: true}));"));
        await("document.getElementById('flightForm')");

        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('flightForm')"
                        + "  .dispatchEvent(new Event('submit', {cancelable: true}));"));

        await("document.getElementById('flightList')");
        await("document.querySelectorAll('#flightList .select-button').length === 2");
    }

    @Test
    void hostileNotificationTextRendersAsLiteralText() throws Exception {
        await("window.App");

        String hostile = "quote ' double \" backslash \\ tag </script><img src=x>";
        runOnFxThread(() -> engine.executeScript(
                "window.showNotification(" + com.owo.utils.Json.quote(hostile) + ")"));

        await("document.querySelector('.notification-content')");

        // Rendered with textContent, so the markup is shown rather than parsed.
        assertEquals(hostile, eval("document.querySelector('.notification-content').textContent"));
        assertEquals(Boolean.FALSE,
                eval("!!document.querySelector('.notification-content img')"));
    }

    @Test
    void screenContentIsEscapedBeforeItReachesTheDocument() throws Exception {
        await("window.App");

        assertEquals("&lt;img src=x onerror=alert(1)&gt;",
                eval("window.App.escapeHtml('<img src=x onerror=alert(1)>')"));
    }

    @Test
    void anUnauthenticatedRequestForBookingsIsRefused() throws Exception {
        await("window.App");

        runOnFxThread(() -> engine.executeScript(
                "window.__result = null;"
                        + "window.OwOAPI.getUserBookings()"
                        + "  .then(function (d) { window.__result = {ok: true}; })"
                        + "  .catch(function (e) { window.__result = {ok: false, code: e.code}; });"));

        await("window.__result");
        assertEquals(Boolean.FALSE, eval("window.__result.ok"));
        assertEquals("ERR_UNAUTHENTICATED", eval("window.__result.code"));
    }

    // ------------------------------------------------------------ hotel & payment

    /** Signs the given account in through the login screen and waits for the home screen. */
    private void signIn(String email, String password) throws Exception {
        await("window.App && document.getElementById('loginForm')");
        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('email').value = " + Json.quote(email) + ";"
                        + "document.getElementById('password').value = " + Json.quote(password) + ";"
                        + "document.getElementById('loginForm')"
                        + "  .dispatchEvent(new Event('submit', {cancelable: true}));"));
        await("document.getElementById('flightForm')");
    }

    private void submitHotelSearch() throws Exception {
        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('hotelForm')"
                        + "  .dispatchEvent(new Event('submit', {cancelable: true}));"));
    }

    @Test
    void selectingAHotelBooksItAndOpensPayment() throws Exception {
        AuthController.register("Sari", "sari@example.com", "password123");
        // The form defaults to a stay of today+1 to today+3, and the search
        // requires the listing window to contain it, so the room opens today.
        Fixtures.hotel(0);

        signIn("sari@example.com", "password123");
        submitHotelSearch();

        await("document.querySelectorAll('#hotelList .select-button').length === 1");
        runOnFxThread(() -> engine.executeScript(
                "document.querySelector('#hotelList .select-button').click()"));

        // The hotel path had no live coverage at all: search, selection by primary key,
        // and the hand-off to payment were only ever reasoned about.
        await("document.getElementById('payment-button')");
        assertEquals("1 orang", eval("document.getElementById('guestParty').textContent"));
        assertEquals("Sari", eval("document.getElementById('guestName').textContent"));
    }

    @Test
    void theHotelSearchHidesRoomsTooSmallForTheParty() throws Exception {
        AuthController.register("Rian", "rian@example.com", "password123");
        Fixtures.hotelSeating(0, 1);
        Fixtures.hotelSeating(0, 4);

        signIn("rian@example.com", "password123");

        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('guests-input').value = '4';"));
        submitHotelSearch();

        // Only the four-guest room can hold the party. Before capacity was modelled the
        // count was discarded and both rooms were offered.
        await("document.querySelectorAll('#hotelList .select-button').length === 1");
    }

    @Test
    void paymentRefusesACardNumberThatFailsTheChecksum() throws Exception {
        AuthController.register("Bayu", "bayu@example.com", "password123");
        Fixtures.flight(3);

        signIn("bayu@example.com", "password123");
        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('flightForm')"
                        + "  .dispatchEvent(new Event('submit', {cancelable: true}));"));
        await("document.querySelectorAll('#flightList .select-button').length === 1");
        runOnFxThread(() -> engine.executeScript(
                "document.querySelector('#flightList .select-button').click()"));
        await("document.getElementById('payment-button')");

        // 4111111111111112 is 4111111111111111 with the check digit broken: the right
        // length and all digits, so only the Luhn check rejects it.
        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('cardName').value = 'Bayu';"
                        + "document.getElementById('cardNumber').value = '4111111111111112';"
                        + "document.getElementById('cvv').value = '123';"
                        + "document.getElementById('expiryMonth').value = '12';"
                        + "document.getElementById('expiryYear').value = '"
                        + (java.time.Year.now().getValue() + 2) + "';"
                        + "document.getElementById('payment-button').click();"));

        await("document.getElementById('screenError')"
                + " && !document.getElementById('screenError').classList.contains('hidden')");
        assertEquals("Nomor kartu tidak valid.",
                eval("document.getElementById('screenError').textContent"));
    }

    @Test
    void aValidCardConfirmsTheBookingAndReachesHistory() throws Exception {
        AuthController.register("Nadia", "nadia@example.com", "password123");
        Fixtures.flight(3);

        signIn("nadia@example.com", "password123");
        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('flightForm')"
                        + "  .dispatchEvent(new Event('submit', {cancelable: true}));"));
        await("document.querySelectorAll('#flightList .select-button').length === 1");
        runOnFxThread(() -> engine.executeScript(
                "document.querySelector('#flightList .select-button').click()"));
        await("document.getElementById('payment-button')");

        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('cardName').value = 'Nadia';"
                        + "document.getElementById('cardNumber').value = '4111111111111111';"
                        + "document.getElementById('cvv').value = '123';"
                        + "document.getElementById('expiryMonth').value = '12';"
                        + "document.getElementById('expiryYear').value = '"
                        + (java.time.Year.now().getValue() + 2) + "';"
                        + "document.getElementById('payment-button').click();"));

        await("document.getElementById('bookingList')");
        await("document.querySelectorAll('#bookingList .booking-card').length === 1");
    }

    // -------------------------------------------------------------- refund review

    @Test
    void aFiledRefundIsVisibleToTheCustomerWhoFiledIt() throws Exception {
        Akun customer = AuthController.register("Tio", "tio@example.com", "password123");
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(20),
                PemesananStatus.CONFIRMED);
        new RefundController().ajukanRefund(booking.getId(), customer.getID(),
                "Perubahan rencana", "Tio", "1234567890");

        signIn("tio@example.com", "password123");
        runOnFxThread(() -> engine.executeScript("window.App.navigate('RefundForm')"));

        // Filing a refund used to be the end of it: nothing could read one back, so the
        // status panel stayed the placeholder markup it shipped as.
        await("document.querySelectorAll('#refundStatusList .refund-card').length === 1");
        assertEquals(Boolean.TRUE,
                eval("document.querySelector('#refundStatusList .refund-card')"
                        + "  .textContent.indexOf('Menunggu peninjauan') >= 0"));
    }

    @Test
    void correctingThePayeeOnAPendingRefundReachesTheDatabase() throws Exception {
        Akun customer = AuthController.register("Wira", "wira@example.com", "password123");
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(20),
                PemesananStatus.CONFIRMED);
        var refund = new RefundController().ajukanRefund(booking.getId(), customer.getID(),
                "Perubahan rencana", "Salah Nama", "0000000000");

        signIn("wira@example.com", "password123");
        runOnFxThread(() -> engine.executeScript("window.App.navigate('RefundForm')"));
        await("document.querySelectorAll('#refundStatusList .refund-card').length === 1");

        // Driven through the control a user actually has, not through the bridge. An
        // earlier version of this test called updateRefundPayee directly and passed while
        // the button was inert: the page used window.prompt, and WebView answers that with
        // an empty string when no prompt handler is installed — which nothing installs.
        runOnFxThread(() -> engine.executeScript(
                "document.querySelector('[data-edit-payee]').click()"));
        await("document.getElementById('payeeNama')");

        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('payeeNama').value = 'Wira Benar';"
                        + "document.getElementById('payeeRekening').value = '9876543210';"
                        + "document.getElementById('payeeSave').click();"));

        // The DAO update behind this had no caller at all, so a wrong account number was
        // permanent once submitted.
        awaitStored(refund.getId(), "Wira Benar", "9876543210");
    }

    /** Polls the stored refund until the payee matches, so the assertion is on the row. */
    private void awaitStored(String refundId, String nama, String rekening) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000;
        Refund stored = null;
        while (System.currentTimeMillis() < deadline) {
            stored = RefundDAO.getRefundById(refundId);
            if (nama.equals(stored.getNamaPenerima())) {
                break;
            }
            Thread.sleep(100);
        }
        assertEquals(nama, stored.getNamaPenerima());
        assertEquals(rekening, stored.getRekeningTujuan());
    }

    @Test
    void cancellingACheckInThenConfirmingAnotherChecksInOnlyTheSecond() throws Exception {
        Akun customer = AuthController.register("Gita", "gita@example.com", "password123");
        // Two bookings whose check-in window is today, so both offer the button.
        Pemesanan first = Fixtures.booking(customer, Fixtures.flight(0),
                PemesananStatus.CONFIRMED);
        Pemesanan second = Fixtures.booking(customer, Fixtures.flight(0),
                PemesananStatus.CONFIRMED);

        signIn("gita@example.com", "password123");
        runOnFxThread(() -> engine.executeScript("window.App.navigate('RiwayatPemesanan')"));
        await("document.querySelectorAll('[data-action=\"checkin\"]').length === 2");

        // Open for the first booking and back out.
        runOnFxThread(() -> engine.executeScript(
                "document.querySelectorAll('[data-action=\"checkin\"]')[0].click()"));
        await("document.getElementById('checkinModal').style.display === 'flex'");
        runOnFxThread(() -> engine.executeScript(
                "document.querySelector('#checkinModal .modal-btn-secondary').click()"));

        // Then open for the second and confirm.
        runOnFxThread(() -> engine.executeScript(
                "document.querySelectorAll('[data-action=\"checkin\"]')[1].click()"));
        await("document.getElementById('checkinModal').style.display === 'flex'");
        runOnFxThread(() -> engine.executeScript(
                "document.querySelector('#checkinModal .modal-btn-primary').click()"));

        // Wait on the page rather than polling the database: reading it from this thread
        // while the bridge is mid-write contends for the file and fails with SQLITE_BUSY.
        await("document.getElementById('modal-success-message').style.display === 'block'");
        await("document.getElementById('bookingList')");

        assertEquals(PemesananStatus.CHECKED_IN, PemesananStatus.fromDb(
                PemesananDAO.getPemesananById(second.getId()).getStatus()));

        // The modal is a single element that is never re-rendered. Attaching its handlers
        // on every open left the cancelled booking's handler in place, so confirming the
        // second checked in the first as well.
        assertEquals(PemesananStatus.CONFIRMED, PemesananStatus.fromDb(
                PemesananDAO.getPemesananById(first.getId()).getStatus()));
    }

    @Test
    void theReviewQueueIsRefusedToAnOrdinaryCustomer() throws Exception {
        AuthController.register("Lina", "lina@example.com", "password123");
        signIn("lina@example.com", "password123");

        runOnFxThread(() -> engine.executeScript(
                "window.__result = null;"
                        + "window.OwOAPI.getPendingRefunds()"
                        + "  .then(function () { window.__result = {ok: true}; })"
                        + "  .catch(function (e) { window.__result = {ok: false, code: e.code}; });"));

        await("window.__result");
        assertEquals(Boolean.FALSE, eval("window.__result.ok"));
        assertEquals("ERR_FORBIDDEN", eval("window.__result.code"));
        // The entry point is not offered either, but that is presentation; the refusal
        // above is what actually protects the operation.
        assertEquals(Boolean.TRUE,
                eval("document.getElementById('reviewButton').classList.contains('hidden')"));
    }

    @Test
    void anAdministratorApprovesAFiledRefundFromTheQueue() throws Exception {
        Akun customer = Fixtures.customer();
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(20),
                PemesananStatus.CONFIRMED);
        new RefundController().ajukanRefund(booking.getId(), customer.getID(),
                "Perubahan rencana", "Penerima", "1234567890");

        Akun admin = Fixtures.admin();
        signIn(admin.getEmail(), "password123");

        assertEquals(Boolean.FALSE,
                eval("document.getElementById('reviewButton').classList.contains('hidden')"));
        runOnFxThread(() -> engine.executeScript(
                "document.getElementById('reviewButton').click()"));

        await("document.querySelectorAll('#refundQueue .review-card').length === 1");
        runOnFxThread(() -> engine.executeScript(
                "document.querySelector('#refundQueue [data-decision=\"approve\"]').click()"));

        // The queue empties because the refund left PENDING_REVIEW, which is the whole
        // point: approval had tested rules and no way to reach them.
        await("document.querySelectorAll('#refundQueue .review-card').length === 0");
        assertEquals(PemesananStatus.REFUNDED,
                PemesananStatus.fromDb(
                        PemesananDAO.getPemesananById(booking.getId()).getStatus()));
    }
}
