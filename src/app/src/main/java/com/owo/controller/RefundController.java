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

    public void setujuiRefund(Refund refund)
            throws PemesananController.PemesananException, SQLException {
        if (refund == null || refund.getStatus() != RefundStatus.PENDING_REVIEW) {
            throw new PemesananController.PemesananException(
                    "Refund ini tidak sedang menunggu peninjauan.");
        }

        RefundDAO.updateStatus(refund.getId(), RefundStatus.APPROVED);
        refund.setStatus(RefundStatus.APPROVED);

        Pemesanan pemesanan = PemesananDAO.getPemesananById(refund.getPemesananID());
        if (pemesanan != null) {
            pemesananController.transition(pemesanan, PemesananStatus.REFUNDED);
            PemesananDAO.releaseTiket(pemesanan.getTiket().getId());
        }
    }

    /**
     * Rejects a refund and restores the booking to whatever it was before.
     *
     * <p>Rejection used to write CONFIRMED unconditionally, which silently downgraded a
     * checked-in booking and re-enabled check-in on it.
     */
    public void tolakRefund(Refund refund)
            throws PemesananController.PemesananException, SQLException {
        if (refund == null || refund.getStatus() != RefundStatus.PENDING_REVIEW) {
            throw new PemesananController.PemesananException(
                    "Refund ini tidak sedang menunggu peninjauan.");
        }

        RefundDAO.updateStatus(refund.getId(), RefundStatus.REJECTED);
        refund.setStatus(RefundStatus.REJECTED);

        Pemesanan pemesanan = PemesananDAO.getPemesananById(refund.getPemesananID());
        if (pemesanan == null) {
            return;
        }

        PemesananStatus restoreTo = refund.getStatusSebelumnya();
        if (restoreTo == null) {
            restoreTo = PemesananStatus.CONFIRMED;
        }
        pemesananController.transition(pemesanan, restoreTo);
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
