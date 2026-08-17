package com.owo;

import java.net.URL;
import java.sql.SQLException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import com.owo.utils.DBHelper;
import com.owo.utils.BridgeInstaller;
import com.owo.utils.JavaScriptBridge;
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
                BridgeInstaller.install(webEngine, javaScriptBridge);

                NotificationBridge.getInstance().setWebEngine(webEngine);

                // Notification polling starts on login, with the session user id.
                // It used to start here, hardcoded to user 1.
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
            BridgeInstaller.shutdown(javaScriptBridge);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
