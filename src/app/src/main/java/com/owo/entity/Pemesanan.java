package com.owo.entity;

import java.time.LocalDate;

public class Pemesanan {
    private String id;
    private String userID;
    private String tiketID;
    private String status;
    private LocalDate tanggalPemesanan;

    public Pemesanan(String id, String userID, String tiketID, String status, LocalDate tanggalPemesanan) {
        this.id = id;
        this.userID = userID;
        this.tiketID = tiketID;
        this.status = status;
        this.tanggalPemesanan = tanggalPemesanan;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getTiketID() {
        return tiketID;
    }

    public void setTiketID(String tiketID) {
        this.tiketID = tiketID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getTanggalPemesanan() {
        return tanggalPemesanan;
    }

    public void setTanggalPemesanan(LocalDate tanggalPemesanan) {
        this.tanggalPemesanan = tanggalPemesanan;
    }
}