package com.owo.entity;

import java.time.LocalDateTime;

public class TiketPesawat extends Tiket {
    private String flightNumber;
    private String maskapai;
    private String origin;
    private String destination;
    private String kelas;
    private LocalDateTime waktuKeberangkatan;

    public TiketPesawat(int id, float harga, boolean tersedia, String flightNumber, String origin, String destination,
            String maskapai, String kelas, LocalDateTime waktuKeberangkatan) {
        super(id, harga, tersedia);
        this.flightNumber = flightNumber;
        this.maskapai = maskapai;
        this.origin = origin;
        this.destination = destination;
        this.waktuKeberangkatan = waktuKeberangkatan;
        this.kelas = kelas;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getMaskapai() {
        return maskapai;
    }

    public void setMaskapai(String maskapai) {
        this.maskapai = maskapai;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }
  
    public LocalDateTime getWaktuKeberangkatan() {
        return waktuKeberangkatan;
    }
    
    public void setWaktuKeberangkatan(LocalDateTime waktuKeberangkatan) {
        this.waktuKeberangkatan = waktuKeberangkatan;
    }
  
    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getKelas() {
        return kelas;
    }

    public void setKelas(String kelas) {
        this.kelas = kelas;
    }
}