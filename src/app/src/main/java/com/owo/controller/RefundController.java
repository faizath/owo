package com.owo.controller;

import com.owo.entity.Pemesanan;
import com.owo.entity.Refund;
import com.owo.entity.Refund.RefundStatus;
import com.owo.entity.Tiket;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RefundController {

    private final Map<Integer, Pemesanan> pemesananTable;
    private final Map<String, Refund> refundTable;

    public RefundController(Map<Integer, Pemesanan> pemesananTable, Map<String, Refund> refundTable) {
        this.pemesananTable = pemesananTable;
        this.refundTable = refundTable;
    }

    public double hitungJumlahRefund(Pemesanan pemesanan) throws Exception {
        Tiket tiket = pemesanan.getTiket();
        LocalDateTime tanggalKeberangkatan = null;

        if (tiket instanceof TiketPesawat) {
            tanggalKeberangkatan = ((TiketPesawat) tiket).getWaktuKeberangkatan();
        } else if (tiket instanceof TiketHotel) {
            tanggalKeberangkatan = ((TiketHotel) tiket).getCheckIn().atStartOfDay();
        }

        if (tanggalKeberangkatan == null) {
            throw new Exception("Tidak dapat menentukan tanggal keberangkatan dari tiket.");
        }

        // Aturan Bisnis:
        // - Refund > 7 hari sebelum appointment: 90% dari harga.
        // - Refund <= 7 hari sebelum appointment: 75% dari harga.
        // - Biaya admin flat Rp 50.000 untuk semua refund.
        long selisihHari = ChronoUnit.DAYS.between(LocalDateTime.now(), tanggalKeberangkatan);
        double persentase = (selisihHari > 7) ? 0.90 : 0.75;
        double biayaAdmin = 50000.0;

        double jumlahRefund = (tiket.getHarga() * persentase) - biayaAdmin;
        return Math.max(0, jumlahRefund);
    }

    public Refund ajukanRefund(int pemesananID, String alasan, Map<String, String> detailKartu) throws Exception {
        Pemesanan pemesanan = this.pemesananTable.get(pemesananID);

        if (pemesanan == null) {
            throw new Exception("Pemesanan dengan ID " + pemesananID + " tidak ditemukan.");
        }
        if (!"CONFIRMED".equalsIgnoreCase(pemesanan.getStatus())) {
            throw new Exception("Refund tidak tersedia. Status pemesanan saat ini: " + pemesanan.getStatus());
        }

        double jumlahRefundFinal = hitungJumlahRefund(pemesanan);
        String refundId = "RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Refund refund = new Refund(refundId, pemesanan.getId(), alasan, jumlahRefundFinal);
        refund.setDetailKartu(
                detailKartu.get("namaKartu"),
                detailKartu.get("nomorKartu"),
                detailKartu.get("expiryMonth"),
                detailKartu.get("expiryYear"),
                detailKartu.get("cvv"));

        this.refundTable.put(refund.getId(), refund);
        pemesanan.setStatus("REFUND_IN_PROGRESS");

        System.out.println("   [SUKSES] Refund berhasil diajukan dengan ID: " + refundId);
        System.out.printf("   -> Jumlah yang akan dikembalikan: IDR %,.2f%n", jumlahRefundFinal);

        return refund;
    }

    public List<Refund> getDaftarRefund() {
        return new ArrayList<>(this.refundTable.values());
    }

    public void setujuiRefund(Refund refund) {
        if (refund != null && refund.getStatus() == RefundStatus.PENDING_REVIEW) {
            refund.setStatus(RefundStatus.APPROVED);
            Pemesanan pemesananTerkait = this.pemesananTable.get(refund.getPemesananID());
            if (pemesananTerkait != null) {
                pemesananTerkait.setStatus("REFUNDED");
            }
        }
    }

    public void tolakRefund(Refund refund) {
        if (refund != null && refund.getStatus() == RefundStatus.PENDING_REVIEW) {
            refund.setStatus(RefundStatus.REJECTED);
            Pemesanan pemesananTerkait = this.pemesananTable.get(refund.getPemesananID());
            if (pemesananTerkait != null) {
                pemesananTerkait.setStatus("CONFIRMED");
            }
        }
    }
}