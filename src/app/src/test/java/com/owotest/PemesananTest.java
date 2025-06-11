package com.owotest;

import com.owo.entity.Pemesanan;
import com.owo.entity.TiketPesawat;
import com.owo.entity.TiketHotel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PemesananTest {
    private Pemesanan pemesananPesawat;
    private Pemesanan pemesananHotel;
    private final int idPesawat = 1;
    private final int idHotel = 2;
    private final String customerId = "C001";
    private final TiketPesawat tiketPesawat = new TiketPesawat(1, 100000.0f, true, "GA123", "Jakarta", "Bali", "Garuda Indonesia", "Ekonomi");
    private final LocalDate checkIn = LocalDate.of(2024, 3, 15);
    private final LocalDate checkOut = LocalDate.of(2024, 3, 20);
    private final TiketHotel tiketHotel = new TiketHotel(2, 2000000.0f, true, checkIn, checkOut, "Grand Hotel", "101", "Jakarta");

    @BeforeEach
    void setUp() {
        pemesananPesawat = new Pemesanan(idPesawat, customerId, tiketPesawat);
        pemesananHotel = new Pemesanan(idHotel, customerId, tiketHotel);
    }

    @Test
    @DisplayName("Test Pemesanan with TiketPesawat creation and getters")
    void testPemesananPesawatCreation() {
        assertNotNull(pemesananPesawat);
        assertEquals(idPesawat, pemesananPesawat.getId());
        assertEquals(customerId, pemesananPesawat.getCustomerId());
        assertEquals(tiketPesawat, pemesananPesawat.getTiket());
        assertEquals("PENDING", pemesananPesawat.getStatus());
        assertNotNull(pemesananPesawat.getTanggalPesan());
    }

    @Test
    @DisplayName("Test Pemesanan with TiketHotel creation and getters")
    void testPemesananHotelCreation() {
        assertNotNull(pemesananHotel);
        assertEquals(idHotel, pemesananHotel.getId());
        assertEquals(customerId, pemesananHotel.getCustomerId());
        assertEquals(tiketHotel, pemesananHotel.getTiket());
        assertEquals("PENDING", pemesananHotel.getStatus());
        assertNotNull(pemesananHotel.getTanggalPesan());
    }

    @Test
    @DisplayName("Test Pemesanan with TiketPesawat setters")
    void testPemesananPesawatSetters() {
        String newCustomerId = "C002";
        TiketPesawat newTiket = new TiketPesawat(3, 200000.0f, true, "GA124", "Bali", "Jakarta", "Garuda Indonesia", "Bisnis");

        pemesananPesawat.setId(3);
        pemesananPesawat.setCustomerId(newCustomerId);
        pemesananPesawat.setTiket(newTiket);
        pemesananPesawat.setStatus("CONFIRMED");

        assertEquals(3, pemesananPesawat.getId());
        assertEquals(newCustomerId, pemesananPesawat.getCustomerId());
        assertEquals(newTiket, pemesananPesawat.getTiket());
        assertEquals("CONFIRMED", pemesananPesawat.getStatus());
    }

    @Test
    @DisplayName("Test Pemesanan with TiketHotel setters")
    void testPemesananHotelSetters() {
        String newCustomerId = "C002";
        LocalDate newCheckIn = LocalDate.of(2024, 4, 1);
        LocalDate newCheckOut = LocalDate.of(2024, 4, 5);
        TiketHotel newTiket = new TiketHotel(4, 3000000.0f, true, newCheckIn, newCheckOut, "New Grand Hotel", "202", "Surabaya");

        pemesananHotel.setId(4);
        pemesananHotel.setCustomerId(newCustomerId);
        pemesananHotel.setTiket(newTiket);
        pemesananHotel.setStatus("CONFIRMED");

        assertEquals(4, pemesananHotel.getId());
        assertEquals(newCustomerId, pemesananHotel.getCustomerId());
        assertEquals(newTiket, pemesananHotel.getTiket());
        assertEquals("CONFIRMED", pemesananHotel.getStatus());
    }

    @Test
    @DisplayName("Test Pemesanan date handling")
    void testPemesananDateHandling() {
        LocalDateTime newDate = LocalDateTime.now().plusDays(1);
        pemesananPesawat.setTanggalPesan(newDate);
        pemesananHotel.setTanggalPesan(newDate);
        
        assertEquals(newDate, pemesananPesawat.getTanggalPesan());
        assertEquals(newDate, pemesananHotel.getTanggalPesan());
    }

    @Test
    @DisplayName("Test Pemesanan toString")
    void testPemesananToString() {
        String expectedPesawat = "Pemesanan{id=1, customerId='C001', tiket=" + tiketPesawat.toString() + 
                                ", tanggalPesan=" + pemesananPesawat.getTanggalPesan() + 
                                ", status='PENDING'}";
        String expectedHotel = "Pemesanan{id=2, customerId='C001', tiket=" + tiketHotel.toString() + 
                             ", tanggalPesan=" + pemesananHotel.getTanggalPesan() + 
                             ", status='PENDING'}";
        
        assertEquals(expectedPesawat, pemesananPesawat.toString());
        assertEquals(expectedHotel, pemesananHotel.toString());
    }
}
