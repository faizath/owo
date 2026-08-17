package com.owo.entity;

/**
 * A refund request against a booking.
 *
 * <p>This deliberately holds <strong>no card data</strong>. The table previously carried
 * {@code nomor_kartu}, {@code expiry_month}, {@code expiry_year} and {@code cvv} in
 * plaintext. Retaining a card verification value after authorisation is prohibited
 * outright, so the capability was removed rather than completed. What a disbursement
 * actually needs is a payee and an account reference.
 */
public class Refund {
    private String id;
    private int pemesananID;
    private String alasan;
    private RefundStatus status;
    private double jumlahRefund;
    private String namaPenerima;
    private String rekeningTujuan;

    /**
     * The booking status to restore if this refund is rejected. Rejection used to write
     * CONFIRMED unconditionally, which silently downgraded a checked-in booking.
     */
    private PemesananStatus statusSebelumnya;

    public Refund(String id, int pemesananID, String alasan, double jumlahRefund) {
        this.id = id;
        this.pemesananID = pemesananID;
        this.alasan = alasan;
        this.status = RefundStatus.PENDING_REVIEW;
        this.jumlahRefund = jumlahRefund;
    }

    public String getId() {
        return id;
    }

    public int getPemesananID() {
        return pemesananID;
    }

    public String getAlasan() {
        return alasan;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public double getJumlahRefund() {
        return jumlahRefund;
    }

    public String getNamaPenerima() {
        return namaPenerima;
    }

    public String getRekeningTujuan() {
        return rekeningTujuan;
    }

    public PemesananStatus getStatusSebelumnya() {
        return statusSebelumnya;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public void setStatusSebelumnya(PemesananStatus statusSebelumnya) {
        this.statusSebelumnya = statusSebelumnya;
    }

    /**
     * Records where the money should go.
     *
     * @param rekeningTujuan a bank account reference, or the last four digits of a masked
     *     card number — never a full PAN
     */
    public void setDetailPencairan(String namaPenerima, String rekeningTujuan) {
        this.namaPenerima = namaPenerima;
        this.rekeningTujuan = rekeningTujuan;
    }

    public String getData() {
        return "Refund ID: " + id + ", Pemesanan ID: " + pemesananID
                + ", Alasan: " + alasan + ", Status: " + status;
    }

    public enum RefundStatus {
        PENDING_REVIEW,
        APPROVED,
        REJECTED,
        PROCESSING,
        COMPLETED,
        FAILED;

        /** @throws IllegalArgumentException on an unrecognised stored value */
        public static RefundStatus fromDb(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Refund status is missing");
            }
            try {
                return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unrecognised refund status: " + value, e);
            }
        }
    }
}
