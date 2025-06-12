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
import com.owo.utils.JavaScriptBridge;

public class App extends Application {

    private JavaScriptBridge javaScriptBridge;

    @Override
    public void start(Stage primaryStage) {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        // Initialize the comprehensive JavaScript bridge
        javaScriptBridge = new JavaScriptBridge();

        // Load the main application HTML file
        URL appHtml = getClass().getResource("/com/owo/boundary/App.html");
        if (appHtml != null) {
            webEngine.load(appHtml.toExternalForm());
        } else {
            System.err.println("HTML file not found. Looking for: /com/owo/boundary/App.html");
        }

        // Set up the bridge when the page loads
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                
                // Set the JSObject reference in the bridge so it can call JavaScript functions
                javaScriptBridge.setJSObject(window);
                
                // Expose the comprehensive bridge to JavaScript
                window.setMember("owoBridge", javaScriptBridge);

                // Connect the NotificationBridge to the WebEngine
                NotificationBridge notificationBridge = NotificationBridge.getInstance();
                notificationBridge.setWebEngine(webEngine);

                // Initialize notification system for user ID 1 (you can change this as needed)
                NotifikasiHelper.initialize(1);

                // Inject JavaScript helper functions
                webEngine.executeScript(
                    "window.owoAPI = {" +
                    "    // Authentication methods" +
                    "    register: function(nama, email, password, callback) {" +
                    "        const data = 'customerId:1,nama:' + nama + ',email:' + email + ',password:' + password;" +
                    "        owoBridge.registerUser(nama, email, password, callback || 'defaultCallback');" +
                    "    }," +
                    "    login: function(email, password, callback) {" +
                    "        owoBridge.loginUser(email, password, callback || 'defaultCallback');" +
                    "    }," +
                    "    // Booking methods" +
                    "    createHotelBooking: function(bookingData, callback) {" +
                    "        const dataStr = Object.keys(bookingData).map(key => key + ':' + bookingData[key]).join(',');" +
                    "        owoBridge.createHotelBooking(dataStr, callback || 'defaultCallback');" +
                    "    }," +
                    "    createFlightBooking: function(bookingData, callback) {" +
                    "        const dataStr = Object.keys(bookingData).map(key => key + ':' + bookingData[key]).join(',');" +
                    "        owoBridge.createFlightBooking(dataStr, callback || 'defaultCallback');" +
                    "    }," +
                    "    getUserBookings: function(userId, callback) {" +
                    "        owoBridge.getUserBookings(userId, callback || 'defaultCallback');" +
                    "    }," +
                    "    // Refund methods" +
                    "    createRefund: function(refundData, callback) {" +
                    "        const dataStr = Object.keys(refundData).map(key => key + ':' + refundData[key]).join(',');" +
                    "        owoBridge.createRefund(dataStr, callback || 'defaultCallback');" +
                    "    }," +
                    "    // Check-in methods" +
                    "    performCheckIn: function(checkInData, callback) {" +
                    "        const dataStr = Object.keys(checkInData).map(key => key + ':' + checkInData[key]).join(',');" +
                    "        owoBridge.performCheckIn(dataStr, callback || 'defaultCallback');" +
                    "    }," +
                    "    // Search methods" +
                    "    searchFlights: function(searchData, callback) {" +
                    "        const dataStr = Object.keys(searchData).map(key => key + ':' + searchData[key]).join(',');" +
                    "        owoBridge.searchFlights(dataStr, callback || 'defaultCallback');" +
                    "    }," +
                    "    searchHotels: function(searchData, callback) {" +
                    "        const dataStr = Object.keys(searchData).map(key => key + ':' + searchData[key]).join(',');" +
                    "        owoBridge.searchHotels(dataStr, callback || 'defaultCallback');" +
                    "    }," +
                    "    // Utility methods" +
                    "    getVersion: function() {" +
                    "        return owoBridge.getVersion();" +
                    "    }," +
                    "    showNotification: function(message) {" +
                    "        owoBridge.showNotification(message);" +
                    "    }" +
                    "};" +
                    
                    // Default callback function for handling responses
                    "window.defaultCallback = function(response) {" +
                    "    console.log('OwO Bridge Response:', response);" +
                    "    try {" +
                    "        const result = JSON.parse(response);" +
                    "        if (result.success) {" +
                    "            console.log('Success:', result.message);" +
                    "            if (result.data) {" +
                    "                console.log('Data:', result.data);" +
                    "            }" +
                    "        } else {" +
                    "            console.error('Error:', result.message);" +
                    "        }" +
                    "    } catch (e) {" +
                    "        console.log('Raw response:', response);" +
                    "    }" +
                    "};" +
                    
                    // Enhanced notification function
                    "window.showNotification = function(message) {" +
                    "    if (window.parent && window.parent.postMessage) {" +
                    "        window.parent.postMessage({" +
                    "            action: 'show_notification'," +
                    "            message: message" +
                    "        }, '*');" +
                    "    }" +
                    "    console.log('Notification:', message);" +
                    "};" +
                    
                    "console.log('OwO Bridge initialized successfully!');" +
                    "console.log('Available methods:', Object.keys(window.owoAPI));"
                );

                System.out.println("JavaScriptBridge initialized and connected to WebView");
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("Failed to load HTML file.");
            }
        });

        // Create the scene and stage
        BorderPane root = new BorderPane(webView);
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("OwO - Hotel & Flight Booking System");
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