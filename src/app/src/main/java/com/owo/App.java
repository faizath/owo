package com.owo;

// import java.io.File;
import java.net.URL;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import com.owo.utils.NotificationBridge;
import com.owo.utils.NotifikasiHelper;

public class App extends Application {

    public static class JavaBridge {
        public void handleClick(String message) {
            System.out.println("Java received message from JavaScript: " + message);
        }
        
        public void testNotification() {
            NotificationBridge.getInstance().sendNotification("Test notification from JavaBridge!");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        // Load HTML as URL
        // File TestViewer = new File("src/main/java/com/owo/boundary/TestViewer.html");
        // webEngine.load(TestViewer.toURI().toString());
        // webEngine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
        //     if (newDoc != null) {
        //         JSObject window = (JSObject) webEngine.executeScript("window");
        //         window.setMember("javaBridge", new JavaBridge());
        //     }
        // });

        // Load HTML as File
        URL TestViewer = getClass().getResource("/com/owo/boundary/App.html");
        if (TestViewer != null) {
            webEngine.load(TestViewer.toExternalForm());
        } else {
            System.err.println("HTML file not found. Looking for: /com/owo/boundary/App.html");
        }
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", new JavaBridge());

                // Connect the NotificationBridge to the WebEngine
                NotificationBridge notificationBridge = NotificationBridge.getInstance();
                notificationBridge.setWebEngine(webEngine);

                // Initialize notification system for user ID 1 (you can change this as needed)
                NotifikasiHelper.initialize(1);

                // Override JS function to call JavaBridge
                // webEngine.executeScript(
                //     "window.onButtonClicked = function() {" +
                //     "    javaBridge.handleClick('Button was clicked! (overridden)');" +
                //     "}"
                // );
            } else {
                System.err.println("HTML file not found.");
            }
        });

        BorderPane root = new BorderPane(webView);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("JavaFX WebView + JS Bridge");
        primaryStage.setScene(scene);
        
        // Add cleanup when the application closes
        primaryStage.setOnCloseRequest(e -> {
            NotifikasiHelper.stop();
            NotificationBridge.getInstance().shutdown();
        });
        
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}