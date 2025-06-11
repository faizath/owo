package com.owo.controller;

import com.owo.entity.Pemesanan;
import com.owo.entity.Tiket;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;
import java.time.LocalDate;

public class CheckInController {

    public boolean validasiCheckIn(Pemesanan pemesanan) {
        if (pemesanan == null) {
            System.err.println("   [GAGAL] Objek pemesanan tidak boleh null.");
            return false;
        }
        System.out.println("\n==> Memvalidasi check-in untuk Pemesanan ID: " + pemesanan.getId());

        if (pemesanan.getTiket() == null) {
            System.err.println("   [GAGAL] Objek tiket pada pemesanan tidak boleh null.");
            return false;
        }

        if (!"CONFIRMED".equalsIgnoreCase(pemesanan.getStatus())) {
            System.err
                    .println("   [GAGAL] Check-in tidak tersedia. Status pemesanan saat ini: " + pemesanan.getStatus());
            return false;
        }

        // Validasi waktu: Check-in hanya bisa dilakukan pada hari H.
        LocalDate tanggalAcara = getTanggalBooking(pemesanan.getTiket());

        if (tanggalAcara == null) {
            System.err.println("   [GAGAL] Tidak dapat menentukan tanggal acara dari tiket.");
            return false;
        }

        if (LocalDate.now().isBefore(tanggalAcara)) {
            System.err.println("   [GAGAL] Check-in hanya bisa dilakukan pada tanggal " + tanggalAcara);
            return false;
        }

        pemesanan.setStatus("CHECKED_IN");
        System.out.println("   [SUKSES] Check-in berhasil! Status pemesanan diubah menjadi: " + pemesanan.getStatus());
        return true;
    }

    private LocalDate getTanggalBooking(Tiket tiket) {
        if (tiket instanceof TiketPesawat) {
            return ((TiketPesawat) tiket).getWaktuKeberangkatan().toLocalDate();
        } else if (tiket instanceof TiketHotel) {
            return ((TiketHotel) tiket).getCheckIn();
        }
        return null;
    }
}