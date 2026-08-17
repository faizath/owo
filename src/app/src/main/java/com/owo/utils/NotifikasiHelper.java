package com.owo.utils;

import com.owo.dao.NotifikasiDAO;
import com.owo.entity.Notifikasi;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Polls for undelivered notifications for the signed-in user.
 *
 * <p>Started on login with the session user id, stopped on logout and on exit. It used to
 * be started from the page-load handler with a literal user id, which both bound polling
 * permanently to user 1 and leaked a new {@link java.util.Timer} on every navigation.
 *
 * <p>A {@code ScheduledExecutorService} replaces {@code Timer} because a {@code Timer} is
 * killed permanently by a single escaping exception from its task.
 */
public class NotifikasiHelper {

    private static final long POLL_SECONDS = 30;

    private static ScheduledExecutorService scheduler;
    private static int currentUserId;

    private NotifikasiHelper() {
    }

    /** Restarts polling for {@code userId}, replacing any run already in progress. */
    public static synchronized void initialize(int userId) {
        stop();
        currentUserId = userId;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "owo-notifikasi");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(
                NotifikasiHelper::checkAndShowNotifications, 0, POLL_SECONDS, TimeUnit.SECONDS);
    }

    private static void checkAndShowNotifications() {
        // Catches Exception, not SQLException: anything escaping this method silently
        // cancels all future runs of the scheduled task.
        try {
            List<Notifikasi> notifications = NotifikasiDAO.getNotifikasiByUserId(currentUserId);

            for (Notifikasi notifikasi : notifications) {
                if (!notifikasi.isTerkirim()) {
                    NotificationBridge.getInstance().sendNotification(notifikasi.getPesan());
                    NotifikasiDAO.markAsTerkirim(notifikasi.getID());
                }
            }
        } catch (Exception e) {
            System.err.println("Notification poll failed: " + e.getMessage());
        }
    }

    public static synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /** @return true while a poller is running. Used by tests to assert a single scheduler. */
    public static synchronized boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }
}
