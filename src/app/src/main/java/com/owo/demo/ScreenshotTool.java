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
    }

    public static class ScreenshotApp extends Application {

    /** Screens worth capturing, plus the login credentials needed to reach the private ones. */
    private static final List<String> SCREENS = List.of(
            "LoginForm", "RegisterForm", "Pemesanan", "CekKetersediaanPesawat",
            "CekKetersediaanHotel", "Pembayaran", "RiwayatPemesanan", "RefundForm",
            "TinjauRefund");

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
            pause(1200, () -> capture(engine, view, 0));
        });
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
        pause(700, () -> {
            try {
                // The router refuses private screens without a session, so log in first
                // and let the shell decide what it is willing to show.
                engine.executeScript(
                        "if (window.App) { try { window.App.navigate('" + screen + "'); }"
                                + " catch (e) { console.error(e); } }");
            } catch (Exception e) {
                System.err.println("Could not navigate to " + screen + ": " + e.getMessage());
            }

            pause(900, () -> {
                write(view, screen);
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
