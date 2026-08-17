package com.owotest;

import com.owo.utils.BridgeInstaller;
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
}
