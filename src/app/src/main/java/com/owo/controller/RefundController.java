package com.owo.controller;

import com.owo.dao.PemesananDAO;
import com.owo.dao.RefundDAO;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.Refund;
import com.owo.entity.Refund.RefundStatus;
import com.owo.entity.Tiket;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Refund eligibility, amount, and the state changes a refund drives.
 *
 * <p>The amount is always computed here from the stored ticket price. A client-supplied
 * figure is ignored rather than validated.
 */
public class RefundController {

    /** Flat administration fee deducted from every refund. */
    public static final double BIAYA_ADMIN = 50_000.0;

    /** Refunds requested more than this far ahead get the higher tier. */
    public static final Duration TIER_BOUNDARY = Duration.ofDays(7);

    public static final double TIER_AWAL = 0.90;
    public static final double TIER_AKHIR = 0.75;

    private final PemesananController pemesananController;

    public RefundController() {
        this(new PemesananController());
    }

    public RefundController(PemesananController pemesananController) {
        this.pemesananController = pemesananController;
    }

    /**
     * @return the payable amount, after the tier percentage and the administration fee
     * @throws PemesananController.PemesananException if the ticket has already departed, or
     *     the fee would consume the whole refund
     */
    public double hitungJumlahRefund(Pemesanan pemesanan)
            throws PemesananController.PemesananException {
        LocalDateTime keberangkatan = getWaktuKeberangkatan(pemesanan.getTiket());
        if (keberangkatan == null) {
            throw new PemesananController.PemesananException(
                    "Tidak dapat menentukan tanggal keberangkatan dari tiket.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!keberangkatan.isAfter(now)) {
            // Day arithmetic used to truncate a negative difference into the lower tier,
            // which made already-departed tickets refundable.
            throw new PemesananController.PemesananException(
                    "Tiket yang sudah berangkat tidak dapat direfund.");
        }

        // Compared as a Duration: 7 days 23 hours is more than seven days, and used to be
        // truncated to 7 and charged the lower tier.
        Duration sisaWaktu = Duration.between(now, keberangkatan);
        double persentase = sisaWaktu.compareTo(TIER_BOUNDARY) > 0 ? TIER_AWAL : TIER_AKHIR;

        double jumlahRefund = (pemesanan.getTiket().getHarga() * persentase) - BIAYA_ADMIN;
        if (jumlahRefund <= 0) {
            // Clamping to zero told the user "Refund berhasil diajukan... IDR 0.00" and
            // moved the booking to REFUND_IN_PROGRESS for nothing.
            throw new PemesananController.PemesananException(
                    "Nilai refund tidak melebihi biaya administrasi sebesar Rp "
                            + String.format(java.util.Locale.ROOT, "%,.0f", BIAYA_ADMIN) + ".");
        }
        return jumlahRefund;
    }

    /**
     * Files a refund against a booking the caller owns.
     *
     * @param namaPenerima payee name; required
     * @param rekeningTujuan bank account reference or a masked card's last four digits.
     *     Never a full card number, and never a CVV.
     */
    public Refund ajukanRefund(int pemesananId, int customerId, String alasan,
            String namaPenerima, String rekeningTujuan)
            throws PemesananController.PemesananException, SQLException {

        if (alasan == null || alasan.trim().isEmpty()) {
            throw new PemesananController.PemesananException("Alasan refund tidak boleh kosong.");
        }
        // The disbursement details used to be dereferenced without a null check, so
        // submitting without them produced an error whose message was literally "null".
        if (namaPenerima == null || namaPenerima.trim().isEmpty()) {
            throw new PemesananController.PemesananException("Nama penerima tidak boleh kosong.");
        }
        if (rekeningTujuan == null || rekeningTujuan.trim().isEmpty()) {
            throw new PemesananController.PemesananException("Rekening tujuan tidak boleh kosong.");
        }

        Pemesanan pemesanan = pemesananController.getOwnedPemesanan(pemesananId, customerId);
        PemesananStatus statusSebelumnya = PemesananController.readStatus(pemesanan);

        if (!statusSebelumnya.canTransitionTo(PemesananStatus.REFUND_IN_PROGRESS)) {
            throw new PemesananController.PemesananException(
                    "Refund tidak tersedia untuk pemesanan berstatus " + statusSebelumnya + ".");
        }

        double jumlahRefundFinal = hitungJumlahRefund(pemesanan);

        // Generated here, not taken from the request payload.
        String refundId = "RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Refund refund = new Refund(refundId, pemesanan.getId(), alasan.trim(), jumlahRefundFinal);
        refund.setDetailPencairan(namaPenerima.trim(), rekeningTujuan.trim());
        refund.setStatusSebelumnya(statusSebelumnya);

        RefundDAO.createRefund(refund);
        pemesananController.transition(pemesanan, PemesananStatus.REFUND_IN_PROGRESS);

        return refund;
    }

    public List<Refund> getRefundByPemesananId(int pemesananId) throws SQLException {
        return RefundDAO.getRefundByPemesananId(pemesananId);
    }

    /** Every refund the customer has filed, across all of their bookings. */
    public List<Refund> getRefundsForCustomer(int customerId) throws SQLException {
        return RefundDAO.getRefundsByCustomerId(customerId);
    }

    /** The review queue: refunds waiting for an administrator to decide. */
    public List<Refund> getRefundsMenungguPeninjauan() throws SQLException {
        return RefundDAO.getRefundsByStatus(RefundStatus.PENDING_REVIEW);
    }

    /**
     * Corrects where an unreviewed refund pays out.
     *
     * <p>Only the customer who filed it may change it, and only while it is still awaiting
     * review — once a decision is made the payee is part of the record.
     *
     * @throws PemesananController.PemesananException if the refund is not the caller's, has
     *     already been decided, or the new details are blank
     */
    public Refund perbaruiDetailPencairan(String refundId, int customerId, String namaPenerima,
            String rekeningTujuan)
            throws PemesananController.PemesananException, SQLException {

        if (namaPenerima == null || namaPenerima.trim().isEmpty()) {
            throw new PemesananController.PemesananException("Nama penerima tidak boleh kosong.");
        }
        if (rekeningTujuan == null || rekeningTujuan.trim().isEmpty()) {
            throw new PemesananController.PemesananException("Rekening tujuan tidak boleh kosong.");
        }

        Refund refund = getOwnedRefund(refundId, customerId);

        // The status is part of the update, not a check preceding it: an approval landing
        // between the two would otherwise redirect a payout that was already signed off.
        boolean updated = RefundDAO.updateDetailPencairan(refundId, namaPenerima.trim(),
                rekeningTujuan.trim(), RefundStatus.PENDING_REVIEW);
        if (!updated) {
            throw new PemesananController.PemesananException(
                    "Detail pencairan hanya dapat diubah selama refund menunggu peninjauan.");
        }

        refund.setDetailPencairan(namaPenerima.trim(), rekeningTujuan.trim());
        refund.setStatus(RefundStatus.PENDING_REVIEW);
        return refund;
    }

    /**
     * Loads a refund the customer owns.
     *
     * <p>A refund that does not exist and one belonging to somebody else produce the same
     * message, so the reply cannot be used to discover which ids are real.
     */
    public Refund getOwnedRefund(String refundId, int customerId)
            throws PemesananController.PemesananException, SQLException {
        Refund refund = refundId == null ? null : RefundDAO.getRefundById(refundId);
        if (refund != null) {
            try {
                pemesananController.getOwnedPemesanan(refund.getPemesananID(), customerId);
                return refund;
            } catch (PemesananController.PemesananException e) {
                // Deliberately re-worded. Letting the booking-level message through would
                // distinguish "no such refund" from "somebody else's refund", which is
                // enough to enumerate valid ids.
                throw new PemesananController.PemesananException(REFUND_TIDAK_DITEMUKAN);
            }
        }
        throw new PemesananController.PemesananException(REFUND_TIDAK_DITEMUKAN);
    }

    /** One wording for missing and for not-yours, so the two cannot be told apart. */
    private static final String REFUND_TIDAK_DITEMUKAN = "Refund tidak ditemukan.";

    /** Approves by id, loading the current row rather than trusting a client-held copy. */
    public Refund setujuiRefund(String refundId)
            throws PemesananController.PemesananException, SQLException {
        Refund refund = refundId == null ? null : RefundDAO.getRefundById(refundId);
        if (refund == null) {
            throw new PemesananController.PemesananException("Refund tidak ditemukan.");
        }
        setujuiRefund(refund);
        return refund;
    }

    /** Rejects by id, loading the current row rather than trusting a client-held copy. */
    public Refund tolakRefund(String refundId)
            throws PemesananController.PemesananException, SQLException {
        Refund refund = refundId == null ? null : RefundDAO.getRefundById(refundId);
        if (refund == null) {
            throw new PemesananController.PemesananException("Refund tidak ditemukan.");
        }
        tolakRefund(refund);
        return refund;
    }

    public void setujuiRefund(Refund refund)
            throws PemesananController.PemesananException, SQLException {
        // Approving releases the ticket: the booking is over and the unit is sellable.
        decide(refund, RefundStatus.APPROVED, PemesananStatus.REFUNDED, true);
    }

    /**
     * Rejects a refund and restores the booking to whatever it was before.
     *
     * <p>Rejection used to write CONFIRMED unconditionally, which silently downgraded a
     * checked-in booking and re-enabled check-in on it.
     */
    public void tolakRefund(Refund refund)
            throws PemesananController.PemesananException, SQLException {
        PemesananStatus restoreTo = refund == null ? null : refund.getStatusSebelumnya();
        if (restoreTo == null) {
            // Rows written before status_sebelumnya existed carry no previous status.
            // CONFIRMED is the only safe guess, and it is the one that downgrades a
            // checked-in booking — so it is used but never silently: see PR-REF-08.
            restoreTo = PemesananStatus.CONFIRMED;
        }
        decide(refund, RefundStatus.REJECTED, restoreTo, false);
    }

    /**
     * Applies a decision to a refund and its booking as one transaction.
     *
     * <p>The state machine is checked here — the booking must be able to reach
     * {@code bookingStatus} — and the writes are handed to a single DAO call that owns one
     * connection, because leaving the refund decided and the booking untouched produces a
     * refund nothing can ever decide again.
     */
    private void decide(Refund refund, RefundStatus decision, PemesananStatus bookingStatus,
            boolean releaseTiket)
            throws PemesananController.PemesananException, SQLException {

        if (refund == null || refund.getStatus() != RefundStatus.PENDING_REVIEW) {
            throw new PemesananController.PemesananException(
                    "Refund ini tidak sedang menunggu peninjauan.");
        }

        Pemesanan pemesanan = PemesananDAO.getPemesananById(refund.getPemesananID());
        if (pemesanan == null) {
            throw new PemesananController.PemesananException(
                    "Pemesanan untuk refund ini tidak ditemukan.");
        }

        PemesananStatus current = PemesananController.readStatus(pemesanan);
        if (current != bookingStatus && !current.canTransitionTo(bookingStatus)) {
            throw new PemesananController.PemesananException(
                    "Pemesanan berstatus " + current + " tidak dapat menjadi "
                            + bookingStatus + ".");
        }

        Integer tiketId = releaseTiket && pemesanan.getTiket() != null
                ? pemesanan.getTiket().getId() : null;

        boolean applied = RefundDAO.applyDecision(refund.getId(), RefundStatus.PENDING_REVIEW,
                decision, pemesanan.getId(), bookingStatus, tiketId);
        if (!applied) {
            // The conditional update matched nothing: somebody else decided it first.
            throw new PemesananController.PemesananException(
                    "Refund ini sudah diputuskan oleh peninjau lain.");
        }

        refund.setStatus(decision);
        pemesanan.setStatus(bookingStatus.dbValue());
    }

    private LocalDateTime getWaktuKeberangkatan(Tiket tiket) {
        if (tiket instanceof TiketPesawat pesawat) {
            return pesawat.getWaktuKeberangkatan();
        } else if (tiket instanceof TiketHotel hotel) {
            return hotel.getCheckIn().atStartOfDay();
        }
        return null;
    }
}
