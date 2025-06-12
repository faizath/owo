package com.owo.entity;

import java.util.ArrayList;

public class Refund {
    private static ArrayList<Refund> allRefund = new ArrayList<>();
    private String id;
    private int pemesananID;
    private String alasan;
    private RefundStatus status;
    private double jumlahRefund;
    private String namaKartu;
    private String nomorKartu;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;

    public Refund(String id, int pemesananID, String alasan, double jumlahRefund) {
        this.id = id;
        this.pemesananID = pemesananID;
        this.alasan = alasan;
        this.status = RefundStatus.PENDING_REVIEW;
        this.jumlahRefund = jumlahRefund;
        allRefund.add(this);
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

    public String getNamaKartu() {
        return namaKartu;
    }

    public String getNomorKartu() {
        return nomorKartu;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public String getCvv() {
        return cvv;
    }

    public String getData() {
        return "Refund ID: " + id + ", Pemesanan ID: " + pemesananID + ", Alasan: " + alasan + ", Status: " + status;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public void setDetailKartu(String namaKartu, String nomorKartu, String expiryMonth, String expiryYear, String cvv) {
        this.namaKartu = namaKartu;
        this.nomorKartu = nomorKartu;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cvv = cvv;
    }

    public String[] getDetailKartu() {
        return new String[] { namaKartu, nomorKartu, expiryMonth, expiryYear, cvv };
    }

    public enum RefundStatus {
        PENDING_REVIEW,
        APPROVED,
        REJECTED,
        PROCESSING,
        COMPLETED,
        FAILED

    }

    public static Refund getRefundByID(String ID) {
        for (Refund refund : allRefund) {
            if (refund.getId().equals(ID)) {
                return refund;
            }
        }
        return null;
    }
}