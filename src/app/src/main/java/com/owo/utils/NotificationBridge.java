package com.owo.utils;

import javafx.scene.web.WebEngine;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationBridge {
    private static NotificationBridge instance;
    private WebEngine webEngine;
    private static final int MAX_DELIVERY_ATTEMPTS = 3;

    /** A queued message plus how many delivery attempts it has already survived. */
    private record Pending(String message, int attempts) {
        Pending retry() {
            return new Pending(message, attempts + 1);
        }
    }

    private final ConcurrentLinkedQueue<Pending> notificationQueue;
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
        
        notificationQueue.offer(new Pending(message, 0));
        
        // If we're connected, try to process immediately
        if (isConnected) {
            processNotifications();
        }
    }

    private void processNotifications() {
        if (!isConnected || webEngine == null) {
            return;
        }

        Pending pending;
        while ((pending = notificationQueue.poll()) != null) {
            final Pending current = pending;
            javafx.application.Platform.runLater(() -> {
                try {
                    // Passed as an argument. Concatenating it into JavaScript source made
                    // every apostrophe in a message a potential syntax error.
                    netscape.javascript.JSObject window =
                            (netscape.javascript.JSObject) webEngine.executeScript("window");
                    window.call("showNotification", current.message);
                } catch (Exception e) {
                    if (current.attempts + 1 < MAX_DELIVERY_ATTEMPTS) {
                        notificationQueue.offer(current.retry());
                    } else {
                        // Unbounded re-queueing against a one-second scheduler spins forever.
                        System.err.println("Dropping notification after "
                                + MAX_DELIVERY_ATTEMPTS + " attempts: " + e.getMessage());
                    }
                }
            });
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