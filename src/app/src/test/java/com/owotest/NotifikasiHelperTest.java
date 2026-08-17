package com.owotest;

import com.owo.dao.NotifikasiDAO;
import com.owo.entity.Akun;
import com.owo.entity.Notifikasi;
import com.owo.utils.NotifikasiHelper;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifikasiHelperTest {

    private TempDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        NotifikasiHelper.stop();
        db.close();
    }

    @Test
    void repeatedInitialisationLeavesExactlyOnePoller() throws Exception {
        Akun user = Fixtures.customer();

        // Each navigation used to re-run the page-load handler and leak another timer.
        for (int i = 0; i < 5; i++) {
            NotifikasiHelper.initialize(user.getID());
        }

        assertTrue(NotifikasiHelper.isRunning());

        NotifikasiHelper.stop();
        assertFalse(NotifikasiHelper.isRunning(),
                "a single stop left a poller behind, so more than one was running");
    }

    @Test
    void pollingStopsOnLogout() throws Exception {
        Akun user = Fixtures.customer();
        NotifikasiHelper.initialize(user.getID());

        NotifikasiHelper.stop();

        assertFalse(NotifikasiHelper.isRunning());
    }

    @Test
    void unsentNotificationsAreDeliveredAndMarked() throws Exception {
        Akun user = Fixtures.customer();
        NotifikasiDAO.createNotifikasi(user.getID(), "Pemesanan Anda dikonfirmasi.");

        NotifikasiHelper.initialize(user.getID());
        // The first poll runs immediately; give it a moment to complete.
        Thread.sleep(1500);

        List<Notifikasi> notifications = NotifikasiDAO.getNotifikasiByUserId(user.getID());
        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).isTerkirim(), "notification was never marked as sent");
    }

    @Test
    void aPollAgainstAMissingDatabaseDoesNotKillThePoller() throws Exception {
        Akun user = Fixtures.customer();
        NotifikasiHelper.initialize(user.getID());
        Thread.sleep(300);

        // Point the helper at a database that has no schema, so the next poll throws.
        // A java.util.Timer would have been cancelled permanently by this.
        com.owo.utils.DBHelper.setDatabasePath(
                java.nio.file.Files.createTempFile("owo-broken-", ".db"));
        Thread.sleep(300);

        assertTrue(NotifikasiHelper.isRunning(), "a failed poll stopped the poller");
        assertDoesNotThrow(() -> NotifikasiHelper.initialize(user.getID()));
    }
}
