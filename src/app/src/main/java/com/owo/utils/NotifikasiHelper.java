package com.owo.utils;

import com.owo.dao.NotifikasiDAO;
import com.owo.entity.Notifikasi;
import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotifikasiHelper {
    private static Timer notificationTimer;
    private static int currentUserId;
    
    public static void initialize(int userId) {
        currentUserId = userId;
        setupNotificationSystem();
    }
    
    private static void setupNotificationSystem() {
        // Start periodic notification check
        notificationTimer = new Timer(true);
        notificationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAndShowNotifications();
            }
        }, 0, 30000); // Check every 30 seconds
    }
    
    private static void checkAndShowNotifications() {
        try {
            List<Notifikasi> notifications = NotifikasiDAO.getNotifikasiByUserId(currentUserId);
            
            for (Notifikasi notifikasi : notifications) {
                if (!notifikasi.isTerkirm()) {
                    showNotification(notifikasi.getPesan());
                    NotifikasiDAO.markAsTerkirm(notifikasi.getID());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void showNotification(String message) {
        try {
            // Use the NotificationBridge to send notifications
            NotificationBridge bridge = NotificationBridge.getInstance();
            bridge.sendNotification(message);
            
            System.out.println("Notification queued: " + message); // Logging
        } catch (Exception e) {
            System.err.println("Error sending notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void stop() {
        if (notificationTimer != null) {
            notificationTimer.cancel();
        }
    }
}
