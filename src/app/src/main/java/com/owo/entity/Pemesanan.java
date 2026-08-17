package com.owo.entity;

import java.time.LocalDateTime;

public class Pemesanan {
    private int id;
    private String customerId;
    private Tiket tiket;
    private LocalDateTime tanggalPesan;
    private String status;

    public Pemesanan(int id, String customerId, Tiket tiket) {
        this.id = id;
        this.customerId = customerId;
        this.tiket = tiket;
        this.tanggalPesan = LocalDateTime.now();
        this.status = "PENDING";    
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Tiket getTiket() {
        return tiket;
    }

    public void setTiket(Tiket tiket) {
        this.tiket = tiket;
    }

    public LocalDateTime getTanggalPesan() {
        return tanggalPesan;
    }

    public void setTanggalPesan(LocalDateTime tanggalPesan) {
        this.tanggalPesan = tanggalPesan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
