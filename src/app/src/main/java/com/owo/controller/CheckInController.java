package com.owo.controller;

import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.Tiket;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;
import com.owo.utils.NotifikasiHelper;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Check-in eligibility and the state change it produces.
 *
 * <p>The rule is same-day: check-in opens on the departure or check-in date and closes at
 * the end of it. Only the "too early" half used to be enforced, so a booking could be
 * checked into months after the flight had gone — and the test suite covered only that
 * same half, which is why it shipped green.
 */
public class CheckInController {

    private final PemesananController pemesananController;

    public CheckInController() {
        this(new PemesananController());
    }

    public CheckInController(PemesananController pemesananController) {
        this.pemesananController = pemesananController;
    }

    /**
     * Validates without persisting. Retained for the entity-level tests.
     *
     * @return true if this booking may be checked into today
     */
    public boolean validasiCheckIn(Pemesanan pemesanan) {
        try {
            assertCheckInAllowed(pemesanan);
            return true;
        } catch (PemesananController.PemesananException e) {
            return false;
        }
    }

    /**
     * Checks a booking in and persists the change.
     *
     * <p>The status change used to be applied to the in-memory object only, so check-in
     * appeared to succeed and then reverted on restart.
     */
    public Pemesanan checkIn(int pemesananId, int customerId)
            throws PemesananController.PemesananException, SQLException {
        Pemesanan pemesanan = pemesananController.getOwnedPemesanan(pemesananId, customerId);
        assertCheckInAllowed(pemesanan);
        pemesananController.transition(pemesanan, PemesananStatus.CHECKED_IN);
        NotifikasiHelper.catat(customerId, "Check-in untuk pemesanan "
                + pemesanan.getKodeBooking() + " berhasil.");
        return pemesanan;
    }

    private void assertCheckInAllowed(Pemesanan pemesanan)
            throws PemesananController.PemesananException {
        if (pemesanan == null) {
            throw new PemesananController.PemesananException("Pemesanan tidak ditemukan.");
        }
        if (pemesanan.getTiket() == null) {
            throw new PemesananController.PemesananException("Tiket pemesanan tidak ditemukan.");
        }

        PemesananStatus status = PemesananController.readStatus(pemesanan);
        if (status != PemesananStatus.CONFIRMED) {
            throw new PemesananController.PemesananException(
                    "Check-in tidak tersedia untuk pemesanan berstatus " + status + ".");
        }

        LocalDate tanggalAcara = getTanggalBooking(pemesanan.getTiket());
        if (tanggalAcara == null) {
            throw new PemesananController.PemesananException(
                    "Tidak dapat menentukan tanggal keberangkatan.");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(tanggalAcara)) {
            throw new PemesananController.PemesananException(
                    "Check-in baru dapat dilakukan pada tanggal " + tanggalAcara + ".");
        }
        if (today.isAfter(tanggalAcara)) {
            throw new PemesananController.PemesananException(
                    "Masa check-in telah berakhir pada tanggal " + tanggalAcara + ".");
        }
    }

    private LocalDate getTanggalBooking(Tiket tiket) {
        if (tiket instanceof TiketPesawat pesawat) {
            return pesawat.getWaktuKeberangkatan().toLocalDate();
        } else if (tiket instanceof TiketHotel hotel) {
            return hotel.getCheckIn();
        }
        return null;
    }
}
