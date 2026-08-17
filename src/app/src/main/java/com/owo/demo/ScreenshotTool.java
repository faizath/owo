package com.owo.demo;

import com.owo.utils.BridgeInstaller;
import com.owo.utils.DBHelper;
import com.owo.utils.JavaScriptBridge;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Renders each screen to a PNG so the layout can actually be looked at.
 *
 * <p>Every automated check in this project reads markup or drives behaviour; none of them
 * can see that a screen renders <em>correctly</em>. Extracting the screens into fragments
 * rescoped each one's {@code body} rules to {@code .screen}, which is exactly the kind of
 * change that is invisible to a test and obvious in an image.
 *
 * <p>Needs a display. Run with {@code ./gradlew screenshots}; output lands in
 * {@code build/screenshots}.
 */
public class ScreenshotTool {

    /**
     * Entry point.
     *
     * <p>Deliberately not the {@link Application} subclass. Launching a class that extends
     * {@code Application} directly makes the JVM demand the JavaFX runtime on the module
     * path, which a plain {@code JavaExec} task does not set up; going through a launcher
     * that merely calls {@code launch} avoids that check.
     */
    public static void main(String[] args) {
        Application.launch(ScreenshotApp.class, args);
        if (ScreenshotApp.hasFailed()) {
            System.exit(1);
        }
    }

    public static class ScreenshotApp extends Application {

    /**
     * Screens to capture: whatever the router will actually serve.
     *
     * <p>This was a hand-written copy of the bridge's allow-list, which is the drift the
     * boundary tests were changed to stop — a screen added to the router and not to the
     * copy would simply never be looked at.
     */
    private static final List<String> SCREENS = BridgeInstaller.routableScreens();

    /**
     * A JavaScript expression producing the navigation context for a screen.
     *
     * <p>Three screens take a context and render a "nothing selected" branch without one.
     * Navigating to them bare produced an image of an error message and reported it as a
     * screenshot, so the three most complex screens in the application were the three
     * nobody had ever seen populated. Each expression resolves to the same shape the real
     * user journey would hand over.
     */
    private static final java.util.Map<String, String> CONTEXTS = java.util.Map.of(
            "CekKetersediaanPesawat",
            "window.OwOAPI.searchFlights({}).then(function (r) {"
                    + " return { criteria: {}, results: r, date: '' }; })",

            "CekKetersediaanHotel",
            "window.OwOAPI.searchHotels({}).then(function (r) {"
                    + " return { criteria: {}, results: r }; })",

            // Payment needs a booking that exists, so one is made and then cancelled again
            // once the image is written; see CLEANUP.
            "Pembayaran",
            "window.OwOAPI.searchFlights({}).then(function (r) {"
                    + " if (!r.length) { return null; }"
                    + " return window.OwOAPI.createBooking(r[0].id, 1).then(function (b) {"
                    + "   window.__shotBooking = b.id;"
                    + "   return { booking: b, tiket: r[0] }; }); })",

            "Informasi", "Promise.resolve({ topik: 'pembatalan' })");

    /**
     * Undoes what a context had to create, once the screenshot is written.
     *
     * <p>The tool runs against the development database. Leaving a PENDING booking behind
     * would hold its unit out of the catalogue — the exact leak the cancel button exists
     * to close — and every later run would claim another one.
     */
    private static final java.util.Map<String, String> CLEANUP = java.util.Map.of(
            "Pembayaran",
            "if (window.__shotBooking) {"
                    + " window.OwOAPI.cancelBooking(window.__shotBooking)"
                    + "   .catch(function (e) { console.error('cleanup failed', e); });"
                    + " window.__shotBooking = null; }");

    private static final int WIDTH = 1400;
    private static final int HEIGHT = 1000;

    private Path outputDirectory;

    @Override
    public void start(Stage stage) throws Exception {
        outputDirectory = Paths.get(
                System.getProperty("owo.screenshot.dir", "build/screenshots"));
        Files.createDirectories(outputDirectory);

        DBHelper.initializeDatabase();

        WebView view = new WebView();
        WebEngine engine = view.getEngine();
        JavaScriptBridge bridge = new JavaScriptBridge();

        stage.setScene(new Scene(view, WIDTH, HEIGHT));
        stage.show();

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                BridgeInstaller.install(engine, bridge);
                signInThenCapture(engine, view);
            } else if (state == Worker.State.FAILED) {
                System.err.println("Could not load App.html: "
                        + engine.getLoadWorker().getException());
                Platform.exit();
            }
        });

        engine.load(getClass().getResource("/com/owo/boundary/App.html").toExternalForm());
    }

    /**
     * Signs in as the seeded administrator before walking the screens.
     *
     * <p>The router sends an unauthenticated visitor back to the login screen, so without
     * this every private screen would be captured as a picture of the login form. The
     * administrator is used rather than the demo customer so the review queue renders too.
     */
    private void signInThenCapture(WebEngine engine, WebView view) {
        pause(900, () -> {
            try {
                engine.executeScript(
                        "window.OwOAPI.login('admin@owo.id', 'admin1234')"
                                + "  .then(function (u) { window.App.setSession(u); })"
                                + "  .catch(function (e) { console.error('login failed', e); });");
            } catch (Exception e) {
                System.err.println("Could not sign in: " + e.getMessage());
            }

            pause(1200, () -> {
                // The router sends an unauthenticated visitor back to the login screen, so
                // without a session every private screen would be captured as a picture of
                // the login form — and the tool would report nine screenshots written. For
                // something whose only job is to show what tests cannot see, quietly
                // producing wrong images is the failure that matters.
                Object session = engine.executeScript("!!(window.App && window.App.session)");
                if (!Boolean.TRUE.equals(session)) {
                    System.err.println("Could not sign in as " + ADMIN_EMAIL
                            + ". Run './gradlew seed' first — without a session every "
                            + "private screen would render as the login form.");
                    Platform.exit();
                    failed = true;
                    return;
                }
                capture(engine, view, 0);
            });
        });
    }

    /** Credentials the seeder creates; see TestDataInserter. */
    private static final String ADMIN_EMAIL = "admin@owo.id";

    /** Set when the run produced nothing usable, so the process can exit non-zero. */
    private static volatile boolean failed;

    static boolean hasFailed() {
        return failed;
    }

    /**
     * Navigates to screen {@code index}, waits for it to settle, writes it, then recurses.
     *
     * <p>Chained rather than looped because each step has to wait for the page, and a loop
     * on the FX thread would block the very rendering it is waiting for.
     */
    private void capture(WebEngine engine, WebView view, int index) {
        if (index >= SCREENS.size()) {
            System.out.println("Wrote " + SCREENS.size() + " screenshots to "
                    + outputDirectory.toAbsolutePath());
            Platform.exit();
            return;
        }

        String screen = SCREENS.get(index);
        String context = CONTEXTS.getOrDefault(screen, "Promise.resolve(null)");

        pause(700, () -> {
            try {
                // The router refuses private screens without a session, so log in first
                // and let the shell decide what it is willing to show. The context is
                // resolved before navigating, because a screen given none renders its
                // "nothing selected" branch and the image says nothing about the layout.
                engine.executeScript(
                        "if (window.App) { Promise.resolve(" + context + ")"
                                + " .then(function (ctx) { window.App.navigate('" + screen
                                + "', ctx); })"
                                + " .catch(function (e) { console.error('context for " + screen
                                + "', e); window.App.navigate('" + screen + "'); }); }");
            } catch (Exception e) {
                System.err.println("Could not navigate to " + screen + ": " + e.getMessage());
            }

            // Longer than the bare navigation needed: a context that queries the database
            // has to come back before there is anything worth photographing.
            pause(1400, () -> {
                write(view, screen);
                String cleanup = CLEANUP.get(screen);
                if (cleanup != null) {
                    try {
                        engine.executeScript(cleanup);
                    } catch (Exception e) {
                        System.err.println("Could not clean up after " + screen + ": "
                                + e.getMessage());
                    }
                }
                capture(engine, view, index + 1);
            });
        });
    }

    private void write(WebView view, String screen) {
        try {
            WritableImage image = view.snapshot(null, null);
            Path file = outputDirectory.resolve(screen + ".png");
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file.toFile());
            System.out.println("  " + file);
        } catch (Exception e) {
            System.err.println("Could not capture " + screen + " ("
                    + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /** Runs {@code action} on the FX thread after {@code millis}, without blocking it. */
    private void pause(int millis, Runnable action) {
        Thread timer = new Thread(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            Platform.runLater(action);
        });
        timer.setDaemon(true);
        timer.start();
    }
}
}
