package com.owo.entity;

public abstract class Tiket {
    private int id;
    private float harga;
    private boolean tersedia;

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
}
