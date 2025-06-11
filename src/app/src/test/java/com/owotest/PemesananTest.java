package com.owotest;

import com.owo.entity.Pemesanan;
import com.owo.entity.TiketPesawat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class PemesananTest {
    private Pemesanan pemesanan;
    private final int id = 1;
    private final String customerId = "C001";
    private final TiketPesawat tiket = new TiketPesawat(1, 100000.0f, true, "GA123", "Jakarta", "Bali", "Garuda Indonesia", LocalDateTime.now().plusDays(1));

    @BeforeEach
    void setUp() {
        pemesanan = new Pemesanan(id, customerId, tiket);
    }

    @Test
    void testConstructor() {
        assertNotNull(pemesanan);
        assertEquals(id, pemesanan.getId());
        assertEquals(customerId, pemesanan.getCustomerId());
        assertEquals(tiket, pemesanan.getTiket());
        assertEquals("PENDING", pemesanan.getStatus());
        assertNotNull(pemesanan.getTanggalPesan());
    }

    @Test
    void testSetAndGetId() {
        int newId = 2;
        pemesanan.setId(newId);
        assertEquals(newId, pemesanan.getId());
    }

    @Test
    void testSetAndGetCustomerId() {
        String newCustomerId = "C002";
        pemesanan.setCustomerId(newCustomerId);
        assertEquals(newCustomerId, pemesanan.getCustomerId());
    }

    @Test
    void testSetAndGetTiket() {
        TiketPesawat newTiket = new TiketPesawat(2, 200000.0f, true, "GA124", "Bali", "Jakarta", "Garuda Indonesia", LocalDateTime.now().plusDays(2));
        pemesanan.setTiket(newTiket);
        assertEquals(newTiket, pemesanan.getTiket());
    }

    @Test
    void testSetAndGetStatus() {
        String newStatus = "CONFIRMED";
        pemesanan.setStatus(newStatus);
        assertEquals(newStatus, pemesanan.getStatus());
    }

    @Test
    void testSetAndGetTanggalPesan() {
        LocalDateTime newDate = LocalDateTime.now().plusDays(1);
        pemesanan.setTanggalPesan(newDate);
        assertEquals(newDate, pemesanan.getTanggalPesan());
    }
}
