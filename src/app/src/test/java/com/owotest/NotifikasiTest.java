package com.owotest;

import com.owo.entity.Notifikasi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

public class NotifikasiTest {
    private Notifikasi notifikasi;
    private static final int TEST_ID = 1;
    private static final int TEST_USER_ID = 1;
    private static final String TEST_PESAN = "Test notification message";
    private static final LocalDateTime TEST_WAKTU = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        notifikasi = new Notifikasi(TEST_ID, TEST_USER_ID, TEST_PESAN, TEST_WAKTU);
    }

    @Test
    @DisplayName("Test Notifikasi constructor and initial values")
    void testConstructor() {
        assertNotNull(notifikasi);
        assertEquals(TEST_ID, notifikasi.getID());
        assertEquals(TEST_USER_ID, notifikasi.getUserID());
        assertEquals(TEST_PESAN, notifikasi.getPesan());
        assertEquals(TEST_WAKTU, notifikasi.getWaktu());
        assertFalse(notifikasi.isTerkirm());
    }

    @Test
    @DisplayName("Test getID method")
    void testGetID() {
        assertEquals(TEST_ID, notifikasi.getID());
    }

    @Test
    @DisplayName("Test getUserID method")
    void testGetUserID() {
        assertEquals(TEST_USER_ID, notifikasi.getUserID());
    }

    @Test
    @DisplayName("Test getPesan method")
    void testGetPesan() {
        assertEquals(TEST_PESAN, notifikasi.getPesan());
    }

    @Test
    @DisplayName("Test getWaktu method")
    void testGetWaktu() {
        assertEquals(TEST_WAKTU, notifikasi.getWaktu());
    }

    @Test
    @DisplayName("Test isTerkirm method initial state")
    void testIsTerkirmInitial() {
        assertFalse(notifikasi.isTerkirm());
    }

    @Test
    @DisplayName("Test setTerkirm method")
    void testSetTerkirm() {
        notifikasi.setTerkirm();
        assertTrue(notifikasi.isTerkirm());
    }

    @Test
    @DisplayName("Test multiple notifications have different IDs")
    void testMultipleNotifications() {
        Notifikasi notifikasi2 = new Notifikasi(2, TEST_USER_ID, "Second notification", TEST_WAKTU);
        assertNotEquals(notifikasi.getID(), notifikasi2.getID());
    }
}
