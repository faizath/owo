package com.owo.utils;

import javafx.scene.web.WebEngine;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationBridge {
    private static NotificationBridge instance;
    private WebEngine webEngine;
    private final ConcurrentLinkedQueue<String> notificationQueue;
    private final ScheduledExecutorService scheduler;
    private boolean isConnected;

    private NotificationBridge() {
        this.notificationQueue = new ConcurrentLinkedQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.isConnected = false;
        
        // Start a scheduler to process notifications periodically
        scheduler.scheduleAtFixedRate(this::processNotifications, 0, 1, TimeUnit.SECONDS);
    }

    public static synchronized NotificationBridge getInstance() {
        if (instance == null) {
            instance = new NotificationBridge();
        }
        return instance;
    }

    public void setWebEngine(WebEngine webEngine) {
        this.webEngine = webEngine;
        this.isConnected = true;
        
        // Process any queued notifications
        processNotifications();
    }

    public void sendNotification(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        notificationQueue.offer(message);
        
        // If we're connected, try to process immediately
        if (isConnected) {
            processNotifications();
        }
    }

    private void processNotifications() {
        if (!isConnected || webEngine == null) {
            return;
        }

        String notification;
        while ((notification = notificationQueue.poll()) != null) {
            final String currentNotification = notification; // Make it effectively final
            try {
                String jsCommand = String.format(
                    "if (window.showNotification) { window.showNotification('%s'); } " +
                    "else { console.log('Notification: %s'); }",
                    currentNotification.replace("'", "\\'").replace("\n", "\\n"),
                    currentNotification.replace("'", "\\'").replace("\n", "\\n")
                );
                
                javafx.application.Platform.runLater(() -> {
                    try {
                        webEngine.executeScript(jsCommand);
                    } catch (Exception e) {
                        System.err.println("Error executing notification script: " + e.getMessage());
                        // Re-queue the notification for retry
                        notificationQueue.offer(currentNotification);
                    }
                });
                
            } catch (Exception e) {
                System.err.println("Error processing notification: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        this.isConnected = false;
        this.webEngine = null;
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public int getQueueSize() {
        return notificationQueue.size();
    }
} 