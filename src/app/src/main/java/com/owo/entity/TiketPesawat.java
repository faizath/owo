package com.owo.entity;

import java.time.LocalDateTime;

public class TiketPesawat extends Tiket {

    private String flightNumber;
    private String maskapai;
    private String origin;
    private String destination;
    private LocalDateTime waktuKeberangkatan;

    public TiketPesawat(int id, float harga, boolean tersedia, String flightNumber, String origin, String destination,
            String maskapai, LocalDateTime waktuKeberangkatan) {
        super(id, harga, tersedia);
        this.flightNumber = flightNumber;
        this.maskapai = maskapai;
        this.origin = origin;
        this.destination = destination;
        this.waktuKeberangkatan = waktuKeberangkatan;
    }

    public String getMaskapai() {
        return maskapai;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getWaktuKeberangkatan() {
        return waktuKeberangkatan;
    }
}
