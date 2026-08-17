package com.owo.controller;

import com.owo.dao.PemesananDAO;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.Tiket;

import java.sql.SQLException;
import java.util.List;

/**
 * Booking lifecycle, enforced against the database.
 *
 * <p>This used to keep bookings in its own {@code HashMap} with its own id counter that
 * restarted at 1, disconnected from the {@code AUTOINCREMENT} column, so its ids collided
 * with real ones. It also had no callers at all.
 */
public class PemesananController {

    /** Raised when a request is refused for a business reason, with a user-safe message. */
    public static class PemesananException extends Exception {
        public PemesananException(String message) {
            super(message);
        }
    }

    public Pemesanan createPemesanan(int customerId, Tiket tiket)
            throws PemesananException, SQLException {
        if (tiket == null) {
            throw new PemesananException("Tiket tidak boleh kosong.");
        }
        if (!tiket.isTersedia()) {
            throw new PemesananException("Tiket sudah tidak tersedia.");
        }

        try {
            return PemesananDAO.createPemesanan(customerId, tiket);
        } catch (SQLException e) {
            // The conditional availability claim lost a race with another booking.
            throw new PemesananException("Tiket sudah tidak tersedia.");
        }
    }

    /**
     * Loads a booking and verifies it belongs to {@code customerId}.
     *
     * @throws PemesananException if it does not exist or belongs to someone else. Both
     *     cases report the same message, so the response cannot be used to probe for
     *     other users' booking ids.
     */
    public Pemesanan getOwnedPemesanan(int pemesananId, int customerId)
            throws PemesananException, SQLException {
        Pemesanan pemesanan = PemesananDAO.getPemesananById(pemesananId);
        if (pemesanan == null || !String.valueOf(customerId).equals(pemesanan.getCustomerId())) {
            throw new PemesananException("Pemesanan tidak ditemukan.");
        }
        return pemesanan;
    }

    public List<Pemesanan> getPemesananByCustomerId(int customerId) throws SQLException {
        return PemesananDAO.getPemesananByCustomerId(customerId);
    }

    /** Marks a booking paid. */
    public Pemesanan konfirmasiPemesanan(int pemesananId, int customerId)
            throws PemesananException, SQLException {
        Pemesanan pemesanan = getOwnedPemesanan(pemesananId, customerId);
        transition(pemesanan, PemesananStatus.CONFIRMED);
        return pemesanan;
    }

    /**
     * Cancels a booking and returns its ticket to the catalogue.
     *
     * <p>Cancellation used to be permitted from any state except {@code CANCELLED}, so a
     * refunded booking could be cancelled, orphaning its refund record.
     */
    public Pemesanan batalkanPemesanan(int pemesananId, int customerId)
            throws PemesananException, SQLException {
        Pemesanan pemesanan = getOwnedPemesanan(pemesananId, customerId);
        transition(pemesanan, PemesananStatus.CANCELLED);
        PemesananDAO.releaseTiket(pemesanan.getTiket().getId());
        return pemesanan;
    }

    /** Applies a status change if the state machine allows it, and persists it. */
    public void transition(Pemesanan pemesanan, PemesananStatus target)
            throws PemesananException, SQLException {
        PemesananStatus current = readStatus(pemesanan);

        if (current == target) {
            throw new PemesananException("Pemesanan sudah berstatus " + target + ".");
        }
        if (!current.canTransitionTo(target)) {
            throw new PemesananException(
                    "Pemesanan berstatus " + current + " tidak dapat diubah menjadi " + target + ".");
        }

        PemesananDAO.updateStatus(pemesanan.getId(), target);
        pemesanan.setStatus(target.dbValue());
    }

    /** Reads the status as an enum, converting a bad stored value into a safe message. */
    public static PemesananStatus readStatus(Pemesanan pemesanan) throws PemesananException {
        try {
            return PemesananStatus.fromDb(pemesanan.getStatus());
        } catch (IllegalArgumentException e) {
            throw new PemesananException("Status pemesanan tidak dikenali.");
        }
    }
}
