package com.owotest;

import com.owo.entity.Pemesanan;
import com.owo.entity.TiketPesawat;
import com.owo.entity.TiketHotel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class PemesananTest {
    private Pemesanan pemesananPesawat;
    private Pemesanan pemesananHotel;
    private final int id = 1;
    private final String customerId = "C001";
    private final TiketPesawat tiketPesawat = new TiketPesawat(1, 100000.0f, true, "GA123", "Jakarta", "Bali", "Garuda Indonesia", "Ekonomi", LocalDateTime.now().plusDays(1));
    private final TiketHotel tiketHotel = new TiketHotel(2, 500000.0f, true, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "Grand Hotel", "101", "Jakarta");

    @BeforeEach
    void setUp() {
        pemesananPesawat = new Pemesanan(id, customerId, tiketPesawat);
        pemesananHotel = new Pemesanan(id + 1, customerId, tiketHotel);
    }

    @Test
    void testConstructor() {
        assertNotNull(pemesananPesawat);
        assertEquals(id, pemesananPesawat.getId());
        assertEquals(customerId, pemesananPesawat.getCustomerId());
        assertEquals(tiketPesawat, pemesananPesawat.getTiket());
        assertEquals("PENDING", pemesananPesawat.getStatus());
        assertNotNull(pemesananPesawat.getTanggalPesan());

        assertNotNull(pemesananHotel);
        assertEquals(id + 1, pemesananHotel.getId());
        assertEquals(customerId, pemesananHotel.getCustomerId());
        assertEquals(tiketHotel, pemesananHotel.getTiket());
        assertEquals("PENDING", pemesananHotel.getStatus());
        assertNotNull(pemesananHotel.getTanggalPesan());
    }

    @Test
    void testSetAndGetId() {
        int newId = 2;
        pemesananPesawat.setId(newId);
        assertEquals(newId, pemesananPesawat.getId());

        pemesananHotel.setId(newId + 1);
        assertEquals(newId + 1, pemesananHotel.getId());
    }

    @Test
    void testSetAndGetCustomerId() {
        String newCustomerId = "C002";
        pemesananPesawat.setCustomerId(newCustomerId);
        assertEquals(newCustomerId, pemesananPesawat.getCustomerId());

        pemesananHotel.setCustomerId(newCustomerId);
        assertEquals(newCustomerId, pemesananHotel.getCustomerId());
    }

    @Test
    void testSetAndGetTiket() {
        TiketPesawat newTiketPesawat = new TiketPesawat(2, 200000.0f, true, "GA124", "Bali", "Jakarta", "Garuda Indonesia", "Ekonomi", LocalDateTime.now().plusDays(2));
        pemesananPesawat.setTiket(newTiketPesawat);
        assertEquals(newTiketPesawat, pemesananPesawat.getTiket());

        TiketHotel newTiketHotel = new TiketHotel(3, 600000.0f, true, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), "Luxury Hotel", "202", "Bali");
        pemesananHotel.setTiket(newTiketHotel);
        assertEquals(newTiketHotel, pemesananHotel.getTiket());
    }

    @Test
    void testSetAndGetStatus() {
        String newStatus = "CONFIRMED";
        pemesananPesawat.setStatus(newStatus);
        assertEquals(newStatus, pemesananPesawat.getStatus());

        pemesananHotel.setStatus(newStatus);
        assertEquals(newStatus, pemesananHotel.getStatus());
    }

    @Test
    void testSetAndGetTanggalPesan() {
        LocalDateTime newDate = LocalDateTime.now().plusDays(1);
        pemesananPesawat.setTanggalPesan(newDate);
        assertEquals(newDate, pemesananPesawat.getTanggalPesan());

        pemesananHotel.setTanggalPesan(newDate);
        assertEquals(newDate, pemesananHotel.getTanggalPesan());
    }
}