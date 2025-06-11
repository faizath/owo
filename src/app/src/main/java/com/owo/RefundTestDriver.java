package com.owo;

import com.owo.controller.RefundController;
import com.owo.db.SampleDatabase;
import com.owo.entity.Pemesanan;
import com.owo.entity.Refund;
import com.owo.entity.Refund.RefundStatus;

import java.util.HashMap;
import java.util.Map;

public class RefundTestDriver {

    public static void main(String[] args) {
        System.out.println("=============================================");
        System.out.println("=== MEMULAI PENGUJIAN REFUND USE CASE ===");
        System.out.println("=============================================");

        RefundController refundController = new RefundController();

        Map<String, String> detailKartu = new HashMap<>();
        detailKartu.put("namaKartu", "Budi Santoso");
        detailKartu.put("nomorKartu", "9876543210987654");
        detailKartu.put("expiryMonth", "10");
        detailKartu.put("expiryYear", "2027");
        detailKartu.put("cvv", "321");

        // --- SKENARIO 1: Pengajuan dan Persetujuan Refund Sukses ---
        runScenario1(refundController, detailKartu);

        // --- SKENARIO 2: Pengajuan dan Penolakan Refund ---
        runScenario2(refundController, detailKartu);

        // --- SKENARIO 3: Pengajuan Refund Gagal (Status Pemesanan Salah) ---
        runScenario3(refundController, detailKartu);

        // --- SKENARIO 4: Pengajuan Refund Gagal (ID Pemesanan Tidak Ada) ---
        runScenario4(refundController, detailKartu);

        System.out.println("\n=============================================");
        System.out.println("=== PENGUJIAN SELESAI ===");
        System.out.println("=============================================");
    }

    private static void runScenario1(RefundController controller, Map<String, String> detailKartu) {
        printHeader("SKENARIO 1: Pengajuan & Persetujuan Refund Sukses (> 7 hari)");
        int idPemesanan = 1; // Tiket Pesawat GA-202
        Pemesanan p = SampleDatabase.pemesananTable.get(idPemesanan);

        System.out.println("Status Awal: " + p.getStatus());

        // 1. Ajukan Refund
        Refund refundDiajukan = controller.ajukanRefund(idPemesanan, "Ada acara keluarga", detailKartu);
        assert refundDiajukan != null : "FAIL: Pengajuan refund seharusnya berhasil!";
        assert refundDiajukan.getStatus() == RefundStatus.PENDING_REVIEW : "FAIL: Status refund awal salah!";
        assert p.getStatus().equals("REFUND_IN_PROGRESS") : "FAIL: Status pemesanan setelah pengajuan salah!";
        System.out.println("   -> Status Pemesanan setelah diajukan: " + p.getStatus());
        System.out.println("   -> Status Refund setelah diajukan: " + refundDiajukan.getStatus());

        // 2. Setujui Refund
        controller.setujuiRefund(refundDiajukan);
        assert refundDiajukan.getStatus() == RefundStatus.APPROVED : "FAIL: Status refund setelah disetujui salah!";
        assert p.getStatus().equals("REFUNDED") : "FAIL: Status pemesanan setelah refund disetujui salah!";
        System.out.println("   -> Status Pemesanan setelah disetujui: " + p.getStatus());
        System.out.println("   -> Status Refund setelah disetujui: " + refundDiajukan.getStatus());

        System.out.println("[HASIL SKENARIO 1: SUKSES]");
    }

    private static void runScenario2(RefundController controller, Map<String, String> detailKartu) {
        printHeader("SKENARIO 2: Pengajuan & Penolakan Refund (<= 7 hari)");
        int idPemesanan = 3; // Tiket Pesawat QZ-7510
        Pemesanan p = SampleDatabase.pemesananTable.get(idPemesanan);

        System.out.println("Status Awal: " + p.getStatus());

        // 1. Ajukan Refund
        Refund refundDiajukan = controller.ajukanRefund(idPemesanan, "Sakit", detailKartu);
        assert refundDiajukan != null : "FAIL: Pengajuan refund seharusnya berhasil!";
        assert p.getStatus().equals("REFUND_IN_PROGRESS") : "FAIL: Status pemesanan setelah pengajuan salah!";
        System.out.println("   -> Status Pemesanan setelah diajukan: " + p.getStatus());

        // 2. Tolak Refund
        controller.tolakRefund(refundDiajukan);
        assert refundDiajukan.getStatus() == RefundStatus.REJECTED : "FAIL: Status refund setelah ditolak salah!";
        assert p.getStatus().equals("CONFIRMED") : "FAIL: Status pemesanan setelah refund ditolak salah!";
        System.out.println("   -> Status Pemesanan setelah ditolak: " + p.getStatus());
        System.out.println("   -> Status Refund setelah ditolak: " + refundDiajukan.getStatus());

        System.out.println("[HASIL SKENARIO 2: SUKSES]");
    }

    private static void runScenario3(RefundController controller, Map<String, String> detailKartu) {
        printHeader("SKENARIO 3: Pengajuan Refund Gagal (Status Pemesanan CANCELLED)");
        int idPemesanan = 4;
        Pemesanan p = SampleDatabase.pemesananTable.get(idPemesanan);

        System.out.println("Status Awal: " + p.getStatus());
        int jumlahRefundSebelum = SampleDatabase.refundTable.size();

        // 1. Coba ajukan refund
        Refund hasilRefund = controller.ajukanRefund(idPemesanan, "Iseng", detailKartu);
        int jumlahRefundSesudah = SampleDatabase.refundTable.size();

        assert hasilRefund == null : "FAIL: Seharusnya tidak ada objek refund yang dibuat!";
        assert p.getStatus().equals("CANCELLED") : "FAIL: Status pemesanan seharusnya tidak berubah!";
        assert jumlahRefundSebelum == jumlahRefundSesudah : "FAIL: Seharusnya tidak ada refund baru di SampleDatabase!";

        System.out.println("Status Akhir: " + p.getStatus());
        System.out.println("[HASIL SKENARIO 3: SUKSES]");
    }

    private static void runScenario4(RefundController controller, Map<String, String> detailKartu) {
        printHeader("SKENARIO 4: Pengajuan Refund Gagal (ID Pemesanan Tidak Ada)");
        int idPemesanan = 999; // ID yang tidak ada di database

        System.out.println("Mencoba refund untuk ID yang tidak ada: " + idPemesanan);

        Refund hasilRefund = controller.ajukanRefund(idPemesanan, "Test", detailKartu);

        assert hasilRefund == null : "FAIL: Seharusnya tidak ada objek refund yang dibuat!";

        System.out.println("[HASIL SKENARIO 4: SUKSES]");
    }

    private static void printHeader(String title) {
        System.out.println("\n-------------------------------------------------");
        System.out.println(title);
        System.out.println("-------------------------------------------------");
    }
}