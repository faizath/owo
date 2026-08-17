package com.owo.entity;

import java.time.LocalDateTime;

public class Pemesanan {
    private int id;
    private String customerId;
    private Tiket tiket;
    private LocalDateTime tanggalPesan;
    private String status;

    /** Party size this booking was made for; never more than the ticket's capacity. */
    private int jumlahPeserta = 1;

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

    /**
     * The booking reference the customer sees.
     *
     * <p>Lives here so a notification and the screen that shows the booking name it the
     * same way; the two used to be able to drift because the boundary built it itself.
     */
    public String getKodeBooking() {
        return "TXN" + id;
    }

    public int getJumlahPeserta() {
        return jumlahPeserta;
    }

    /** @throws IllegalArgumentException if the party size is not at least one */
    public void setJumlahPeserta(int jumlahPeserta) {
        if (jumlahPeserta < 1) {
            throw new IllegalArgumentException(
                    "Jumlah peserta minimal 1, bukan " + jumlahPeserta);
        }
        this.jumlahPeserta = jumlahPeserta;
    }

}
