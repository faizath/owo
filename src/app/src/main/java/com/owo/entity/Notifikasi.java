package com.owo.entity;

import java.util.ArrayList;
import java.time.LocalDateTime;

public class Notifikasi {
    private static ArrayList<Notifikasi> allNotifikasi = new ArrayList<>();
    private int ID;
    private int userID;
    private String pesan;
    private LocalDateTime waktu;
    private boolean terkirm;

    public Notifikasi(int userID, String pesan) {
        this.ID = allNotifikasi.size() + 1;
        this.userID = userID;
        this.pesan = pesan;
        this.waktu = LocalDateTime.now();
        this.terkirm = false;
        allNotifikasi.add(this);
    }

    public int getID() {
        return ID;
    }

    public int getUserID() {
        return userID;
    }

    public String getPesan() {
        return pesan;
    }

    public LocalDateTime getWaktu() {
        return waktu;
    }

    public boolean isTerkirm() {
        return terkirm;
    }
    
    public void setTerkirm() {
        this.terkirm = true;
    }
}
