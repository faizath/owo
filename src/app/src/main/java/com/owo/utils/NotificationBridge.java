package com.owo.utils;

import javafx.scene.web.WebEngine;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Holds notifications until the page can show them, and retries the ones that fail.
 *
 * <p>Delivery is reached through a {@link Dispatcher} rather than a {@code WebEngine}
 * directly. The thread hop is part of that seam and not part of this class, because
 * delivery must happen on the JavaFX application thread and the failure it can raise
 * happens there too — a seam that replaced only the script call would leave the
 * retry-then-drop path as untestable as it was before.
 */
public class NotificationBridge {
    private static NotificationBridge instance;
    private static final int MAX_DELIVERY_ATTEMPTS = 3;

    /**
     * Where a notification is delivered, and on which thread.
     *
     * <p>Implementations are expected to be cheap to ask about readiness: it is checked on
     * every scheduler tick.
     */
    public interface Dispatcher {
        /** False while there is nothing to deliver through; the queue is then left alone. */
        boolean isReady();

        /** Runs {@code task} on the thread delivery has to happen on. */
        void onDeliveryThread(Runnable task);

        /**
         * Shows one message. Called on the delivery thread.
         *
         * @throws Exception if the page could not be reached; the message is then retried
         */
        void deliver(String message) throws Exception;
    }

    /** A queued message plus how many delivery attempts it has already survived. */
    private record Pending(String message, int attempts) {
        Pending retry() {
            return new Pending(message, attempts + 1);
        }
    }

    private final ConcurrentLinkedQueue<Pending> notificationQueue;
    private final ScheduledExecutorService scheduler;
    private volatile Dispatcher dispatcher;
    private volatile boolean isConnected;

    private NotificationBridge() {
        this.notificationQueue = new ConcurrentLinkedQueue<>();
        // Daemon, as NotifikasiHelper's poller already is. The default factory produces a
        // non-daemon thread, and this singleton is created lazily by the first
        // notification poll — so anything that signs in and does not call shutdown()
        // never exits. ScreenshotTool did exactly that: it wrote all nine images and then
        // hung, which looks like a broken tool rather than a stray thread.
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "owo-notifikasi-delivery");
            t.setDaemon(true);
            return t;
        });
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

    /** Delivers through {@code webEngine} from now on, and flushes anything waiting. */
    public void setWebEngine(WebEngine webEngine) {
        setDispatcher(new WebEngineDispatcher(webEngine));
    }

    /**
     * Replaces the delivery target and flushes the queue through it.
     *
     * <p>Public because this class is never handed to {@code setMember} — only
     * {@link JavaScriptBridge} is exposed to the page — so widening it here reaches no
     * client that could misuse it.
     */
    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
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
        Dispatcher target = dispatcher;
        if (!isConnected || target == null || !target.isReady()) {
            return;
        }

        Pending pending;
        while ((pending = notificationQueue.poll()) != null) {
            final Pending current = pending;
            target.onDeliveryThread(() -> attemptDelivery(target, current));
        }
    }

    /** Delivers one message, re-queueing it until the attempt limit is reached. */
    private void attemptDelivery(Dispatcher target, Pending pending) {
        try {
            target.deliver(pending.message());
        } catch (Exception e) {
            if (pending.attempts() + 1 < MAX_DELIVERY_ATTEMPTS) {
                notificationQueue.offer(pending.retry());
            } else {
                // Unbounded re-queueing against a one-second scheduler spins forever.
                System.err.println("Dropping notification after "
                        + MAX_DELIVERY_ATTEMPTS + " attempts: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        this.isConnected = false;
        this.dispatcher = null;
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

    /** Calls {@code window.showNotification} on the JavaFX application thread. */
    private record WebEngineDispatcher(WebEngine webEngine) implements Dispatcher {

        @Override
        public boolean isReady() {
            return webEngine != null;
        }

        @Override
        public void onDeliveryThread(Runnable task) {
            javafx.application.Platform.runLater(task);
        }

        @Override
        public void deliver(String message) {
            // Passed as an argument. Concatenating it into JavaScript source made every
            // apostrophe in a message a potential syntax error.
            netscape.javascript.JSObject window =
                    (netscape.javascript.JSObject) webEngine.executeScript("window");
            window.call("showNotification", message);
        }
    }
}
