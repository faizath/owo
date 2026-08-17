package com.owo;

import java.net.URL;
import java.sql.SQLException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import com.owo.utils.DBHelper;
import com.owo.utils.JavaScriptBridge;
import com.owo.utils.JsConsole;
import com.owo.utils.NotificationBridge;
import com.owo.utils.NotifikasiHelper;

public class App extends Application {

    private JavaScriptBridge javaScriptBridge;

    @Override
    public void start(Stage primaryStage) {
        // Create the schema before anything can query it. Nothing used to call this,
        // so the first database action on a clean machine threw "no such table: akun".
        try {
            DBHelper.initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Cannot initialise the database at " + DBHelper.getDatabasePath()
                    + ": " + e.getMessage());
            throw new IllegalStateException("Database initialisation failed", e);
        }

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        javaScriptBridge = new JavaScriptBridge();

        URL appHtml = getClass().getResource("/com/owo/boundary/App.html");
        if (appHtml != null) {
            webEngine.load(appHtml.toExternalForm());
        } else {
            System.err.println("HTML file not found. Looking for: /com/owo/boundary/App.html");
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");

                javaScriptBridge.setJSObject(window);

                // Installed before the bridge, so a failure in the page's own startup is
                // still reported rather than silently leaving a blank screen.
                window.setMember("owoConsole", new JsConsole());
                webEngine.executeScript(JsConsole.installScript());

                // Java's only injection responsibility. The JavaScript API layer lives in
                // owo-bridge.js, which is a real file that can be read and linted; it used
                // to be eighty lines embedded in a Java string literal here.
                window.setMember("owoBridge", javaScriptBridge);

                NotificationBridge.getInstance().setWebEngine(webEngine);

                // Notification polling starts on login, with the session user id.
                // It used to start here, hardcoded to user 1.
                webEngine.executeScript("window.dispatchEvent(new Event('owo:bridge-ready'))");
                System.out.println("Bridge ready; database at " + DBHelper.getDatabasePath());
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("Failed to load HTML file.");
            }
        });

        BorderPane root = new BorderPane(webView);
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("OwO - Hotel & Flight Booking System");
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            NotifikasiHelper.stop();
            NotificationBridge.getInstance().shutdown();
            javaScriptBridge.shutdown();
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
