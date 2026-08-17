package com.owo.entity;

public abstract class Tiket {
    private int id;
    private float harga;
    private boolean tersedia;

    /**
     * How many people this bookable unit holds — passengers for a flight, guests for a
     * hotel room. One {@code tiket} row is one bookable unit, so a party larger than this
     * cannot be seated by a single booking and the search filters the row out.
     */
    private int kapasitas = 1;

    public Tiket(int id, float harga, boolean tersedia) {
        this.id = id;
        this.harga = harga;
        this.tersedia = tersedia;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getHarga() {
        return harga;
    }

    public void setHarga(float harga) {
        this.harga = harga;
    }

    public boolean isTersedia() {
        return tersedia;
    }

    public void setTersedia(boolean tersedia) {
        this.tersedia = tersedia;
    }

    public int getKapasitas() {
        return kapasitas;
    }

    /** @throws IllegalArgumentException if the capacity is not at least one */
    public void setKapasitas(int kapasitas) {
        if (kapasitas < 1) {
            throw new IllegalArgumentException("Kapasitas tiket minimal 1, bukan " + kapasitas);
        }
        this.kapasitas = kapasitas;
    }

}
