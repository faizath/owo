package com.owo.entity;

import java.util.ArrayList;

public abstract class Tiket {
    private static ArrayList<Tiket> allTiket = new ArrayList<>();
    private int id;
    private float harga;
    private boolean tersedia;

    public Tiket(int id, float harga, boolean tersedia) {
        this.id = id;
        this.harga = harga;
        this.tersedia = tersedia;
        allTiket.add(this);
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

    public static Tiket getTiketByID(int ID) {
        for (Tiket tiket : allTiket) {
            if (tiket.getId() == ID) {
                return tiket;
            }
        }
        return null;
    }
}
