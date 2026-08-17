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

        TiketPesawat tiketHariIni = TiketPesawat.builder()
                .id(101)
                .harga(1500000f)
                .tersedia(true)
                .flightNumber("GA-202")
                .origin("CGK")
                .destination("DPS")
                .maskapai("Garuda Indonesia")
                .kelas("Ekonomi")
                .waktuKeberangkatan(LocalDateTime.now())
                .build();
        pemesananBisaCheckIn = new Pemesanan(1, "1", tiketHariIni);
        pemesananBisaCheckIn.setStatus("CONFIRMED");

        TiketPesawat tiketBesok = TiketPesawat.builder()
                .id(102)
                .harga(1200000f)
                .tersedia(true)
                .flightNumber("QZ-7510")
                .origin("SUB")
                .destination("CGK")
                .maskapai("AirAsia")
                .kelas("Ekonomi")
                .waktuKeberangkatan(LocalDateTime.now().plusDays(1))
                .build();
        pemesananTerlaluAwal = new Pemesanan(2, "2", tiketBesok);
        pemesananTerlaluAwal.setStatus("CONFIRMED");

        TiketPesawat tiketStatusSalah = TiketPesawat.builder()
                .id(103)
                .harga(950000f)
                .tersedia(true)
                .flightNumber("SJ-182")
                .origin("CGK")
                .destination("PNK")
                .maskapai("Sriwijaya Air")
                .kelas("Bisnis")
                .waktuKeberangkatan(LocalDateTime.now())
                .build();
        pemesananStatusSalah = new Pemesanan(3, "3", tiketStatusSalah);
        pemesananStatusSalah.setStatus("PENDING");

        TiketHotel tiketHotelHariIni = new TiketHotel(201, 800000f, true, LocalDate.now(), LocalDate.now().plusDays(2),
                "Hotel Owo", "101", "Jalan Merdeka No. 45");
        pemesananHotelBisaCheckIn = new Pemesanan(4, "4", tiketHotelHariIni);
        pemesananHotelBisaCheckIn.setStatus("CONFIRMED");
    }

    @Test
    @DisplayName("Check-in Seharusnya Berhasil untuk Pemesanan yang Valid")
    void testValidCheckInSuccess() {
        boolean hasil = checkInController.validasiCheckIn(pemesananBisaCheckIn);

        assertTrue(hasil, "validasiCheckIn seharusnya mengembalikan true untuk pemesanan yang valid.");
        // validasiCheckIn hanya memeriksa kelayakan. Perubahan status dilakukan oleh
        // checkIn(), yang juga menyimpannya ke basis data.
        assertEquals("CONFIRMED", pemesananBisaCheckIn.getStatus(),
                "Validasi saja seharusnya tidak mengubah status.");
    }

    @Test
    @DisplayName("Check-in Seharusnya Berhasil untuk Hotel pada Hari H")
    void testValidHotelCheckInSuccess() {
        boolean hasil = checkInController.validasiCheckIn(pemesananHotelBisaCheckIn);
        assertTrue(hasil, "Check-in hotel pada hari H seharusnya berhasil.");
        assertEquals("CONFIRMED", pemesananHotelBisaCheckIn.getStatus(),
                "Validasi saja seharusnya tidak mengubah status.");
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
