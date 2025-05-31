package com.owo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

// import java.io.File;
import java.net.URL;

public class App extends Application {

    public static class JavaBridge {
        public void handleClick(String message) {
            System.out.println("Java received message from JavaScript: " + message);
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
        URL TestViewer = getClass().getResource("/com/owo/boundary/TestViewer.html");
        if (TestViewer != null) {
            webEngine.load(TestViewer.toExternalForm());
        } else {
            System.err.println("HTML file not found.");
        }
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", new JavaBridge());

                // Override JS function to call JavaBridge
                webEngine.executeScript(
                    "window.onButtonClicked = function() {" +
                    "    javaBridge.handleClick('Button was clicked! (overridden)');" +
                    "}"
                );
            }
        });

        BorderPane root = new BorderPane(webView);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("JavaFX WebView + JS Bridge");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}