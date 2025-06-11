package com.owotest;

import com.owo.controller.CheckInController;
import com.owo.entity.Pemesanan;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CheckInTest {

    private CheckInController checkInController;
    private Pemesanan pemesananBisaCheckIn;
    private Pemesanan pemesananTerlaluAwal;
    private Pemesanan pemesananStatusSalah;
    private Pemesanan pemesananHotelBisaCheckIn;

    @BeforeEach
    void setUp() {
        checkInController = new CheckInController();

        TiketPesawat tiketHariIni = new TiketPesawat(101, 1500000f, true, "GA-202", "CGK", "DPS", "Garuda Indonesia",
                "Ekonomi", LocalDateTime.now());
        pemesananBisaCheckIn = new Pemesanan(1, "cust-01", tiketHariIni);
        pemesananBisaCheckIn.setStatus("CONFIRMED");

        TiketPesawat tiketBesok = new TiketPesawat(102, 1200000f, true, "QZ-7510", "SUB", "CGK", "AirAsia", "Ekonomi",
                LocalDateTime.now().plusDays(1));
        pemesananTerlaluAwal = new Pemesanan(2, "cust-02", tiketBesok);
        pemesananTerlaluAwal.setStatus("CONFIRMED");

        TiketPesawat tiketStatusSalah = new TiketPesawat(103, 950000f, true, "SJ-182", "CGK", "PNK", "Sriwijaya Air",
                "Bisnis", LocalDateTime.now());
        pemesananStatusSalah = new Pemesanan(3, "cust-03", tiketStatusSalah);
        pemesananStatusSalah.setStatus("PENDING");

        TiketHotel tiketHotelHariIni = new TiketHotel(201, 800000f, true, LocalDate.now(), LocalDate.now().plusDays(2),
                "Hotel Owo", "101", "Jalan Merdeka No. 45");
        pemesananHotelBisaCheckIn = new Pemesanan(4, "cust-04", tiketHotelHariIni);
        pemesananHotelBisaCheckIn.setStatus("CONFIRMED");
    }

    @Test
    @DisplayName("Check-in Seharusnya Berhasil untuk Pemesanan yang Valid")
    void testValidCheckInSuccess() {
        boolean hasil = checkInController.validasiCheckIn(pemesananBisaCheckIn);

        assertTrue(hasil, "validasiCheckIn seharusnya mengembalikan true untuk pemesanan yang valid.");
        assertEquals("CHECKED_IN", pemesananBisaCheckIn.getStatus(),
                "Status pemesanan seharusnya berubah menjadi 'CHECKED_IN'.");
    }

    @Test
    @DisplayName("Check-in Seharusnya Berhasil untuk Hotel pada Hari H")
    void testValidHotelCheckInSuccess() {
        boolean hasil = checkInController.validasiCheckIn(pemesananHotelBisaCheckIn);
        assertTrue(hasil, "Check-in hotel pada hari H seharusnya berhasil.");
        assertEquals("CHECKED_IN", pemesananHotelBisaCheckIn.getStatus(),
                "Status pemesanan hotel seharusnya berubah menjadi 'CHECKED_IN'.");
    }

    @Test
    @DisplayName("Check-in Seharusnya Gagal jika Dilakukan Terlalu Awal")
    void testCheckInFailsIfTooEarly() {
        boolean hasil = checkInController.validasiCheckIn(pemesananTerlaluAwal);

        assertFalse(hasil, "validasiCheckIn seharusnya mengembalikan false jika tanggal belum sesuai.");
        assertEquals("CONFIRMED", pemesananTerlaluAwal.getStatus(),
                "Status pemesanan seharusnya tidak berubah jika check-in gagal.");
    }

    @Test
    @DisplayName("Check-in Seharusnya Gagal jika Status Pemesanan Tidak 'CONFIRMED'")
    void testCheckInFailsWithWrongStatus() {
        boolean hasil = checkInController.validasiCheckIn(pemesananStatusSalah);

        assertFalse(hasil, "validasiCheckIn seharusnya mengembalikan false untuk status PENDING.");
        assertEquals("PENDING", pemesananStatusSalah.getStatus(), "Status pemesanan seharusnya tidak berubah.");
    }

    @Test
    @DisplayName("Validasi Check-in Seharusnya Menangani Input Null dengan Aman")
    void testCheckInHandlesNullInput() {
        boolean hasil = checkInController.validasiCheckIn(null);
        assertFalse(hasil, "validasiCheckIn seharusnya mengembalikan false jika input pemesanan adalah null.");
    }
}
