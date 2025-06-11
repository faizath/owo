package com.owo.controller;

import com.owo.entity.Pemesanan;
import com.owo.entity.Refund;
import com.owo.entity.Refund.RefundStatus;
import com.owo.entity.Tiket;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;
import com.owo.App;
import com.owo.db.SampleDatabase;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public class RefundController {

    private Pemesanan pemesanan;
    private Refund refund;
    private App viewer;

    public void setViewer(App viewer) {
        this.viewer = viewer;
    }

    public double hitungJumlahRefund(Pemesanan pemesanan) {
        Tiket tiket = pemesanan.getTiket();
        LocalDateTime tanggalKeberangkatan = null;

        if (tiket instanceof TiketPesawat) {
            tanggalKeberangkatan = ((TiketPesawat) tiket).getWaktuKeberangkatan();
        } else if (tiket instanceof TiketHotel) {
            tanggalKeberangkatan = ((TiketHotel) tiket).getCheckIn().atStartOfDay();
        }

        if (tanggalKeberangkatan == null) {
            System.err.println("Tidak bisa menentukan tanggal acara untuk tiket ID: " + tiket.getId());
            return 0.0;
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

    public Refund ajukanRefund(int pemesananID, String alasan, Map<String, String> detailKartu) {
        System.out.println("\n==> Memproses pengajuan refund untuk Pemesanan ID: " + pemesananID);
        Pemesanan pemesanan = SampleDatabase.pemesananTable.get(pemesananID);

        if (pemesanan == null) {
            System.err.println("   [GAGAL] Pemesanan dengan ID " + pemesananID + " tidak ditemukan.");
            return null;
        }

        if (!"CONFIRMED".equalsIgnoreCase(pemesanan.getStatus())) {
            System.err.println("   [GAGAL] Refund tidak tersedia. Status pemesanan saat ini: " + pemesanan.getStatus());
            return null;
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

        SampleDatabase.refundTable.put(refund.getId(), refund);

        pemesanan.setStatus("REFUND_IN_PROGRESS");

        System.out.println("   [SUKSES] Refund berhasil diajukan dengan ID: " + refundId);
        System.out.printf("   -> Jumlah yang akan dikembalikan: IDR %,.2f%n", jumlahRefundFinal);

        return refund;
    }

    public void getDaftarRefund() {
        JSONArray daftarRefundJson = new JSONArray();

        for (Refund refund : SampleDatabase.refundTable.values()) {

            JSONObject refundJson = new JSONObject();
            refundJson.put("id", refund.getId());
            refundJson.put("pemesananID", refund.getPemesananID());
            refundJson.put("alasan", refund.getAlasan());
            refundJson.put("status", refund.getStatus());
            refundJson.put("jumlah", String.format("IDR %,.0f", refund.getJumlahRefund()));
            refundJson.put("detailKartu", refund.getDetailKartu());

            daftarRefundJson.put(refundJson);
        }
    }

    public void setujuiRefund(Refund refund) {
        if (refund != null && refund.getStatus() == RefundStatus.PENDING_REVIEW) {
            refund.setStatus(RefundStatus.APPROVED);

            Pemesanan pemesananTerkait = SampleDatabase.pemesananTable.get(refund.getPemesananID());
            if (pemesananTerkait != null) {
                pemesananTerkait.setStatus("REFUNDED");
            }
            System.out.println("   [INFO] Refund " + refund.getId()
                    + " telah disetujui. Status pemesanan diubah menjadi REFUNDED.");
        }
    }

    public void tolakRefund(Refund refund) {
        if (refund != null && refund.getStatus() == RefundStatus.PENDING_REVIEW) {
            refund.setStatus(RefundStatus.REJECTED);

            Pemesanan pemesananTerkait = SampleDatabase.pemesananTable.get(refund.getPemesananID());
            if (pemesananTerkait != null) {
                pemesananTerkait.setStatus("CONFIRMED");
            }
            System.out.println(
                    "   [INFO] Refund " + refund.getId() + " ditolak. Status pemesanan dikembalikan ke CONFIRMED.");
        }
    }
}