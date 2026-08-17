package com.owo.entity;

import java.time.LocalDateTime;

/**
 * A bookable seat on a flight.
 *
 * <p>Construct with {@link #builder()}. Five of this type's fields are adjacent strings —
 * flight number, origin, destination, airline and class — and a positional constructor
 * makes any two of them silently interchangeable at the call site, which the compiler
 * cannot catch. The builder names each one.
 */
public class TiketPesawat extends Tiket {
    private String flightNumber;
    private String maskapai;
    private String origin;
    private String destination;
    private String kelas;
    private LocalDateTime waktuKeberangkatan;

    private TiketPesawat(Builder b) {
        super(b.id, b.harga, b.tersedia);
        this.flightNumber = b.flightNumber;
        this.maskapai = b.maskapai;
        this.origin = b.origin;
        this.destination = b.destination;
        this.kelas = b.kelas;
        this.waktuKeberangkatan = b.waktuKeberangkatan;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Names every field, so no two strings can be transposed unnoticed. */
    public static final class Builder {
        private int id;
        private float harga;
        private boolean tersedia = true;
        private String flightNumber;
        private String maskapai;
        private String origin;
        private String destination;
        private String kelas;
        private LocalDateTime waktuKeberangkatan;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder harga(float harga) {
            this.harga = harga;
            return this;
        }

        public Builder tersedia(boolean tersedia) {
            this.tersedia = tersedia;
            return this;
        }

        public Builder flightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public Builder maskapai(String maskapai) {
            this.maskapai = maskapai;
            return this;
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder kelas(String kelas) {
            this.kelas = kelas;
            return this;
        }

        public Builder waktuKeberangkatan(LocalDateTime waktuKeberangkatan) {
            this.waktuKeberangkatan = waktuKeberangkatan;
            return this;
        }

        public TiketPesawat build() {
            return new TiketPesawat(this);
        }
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