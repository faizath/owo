package com.owo.entity;

import java.time.LocalDateTime;

public class Notifikasi {
    private int ID;
    private int userID;
    private String pesan;
    private LocalDateTime waktu;
    private boolean terkirim;

    public Notifikasi(int ID, int userID, String pesan) {
        this.ID = ID;
        this.userID = userID;
        this.pesan = pesan;
        this.waktu = LocalDateTime.now();
        this.terkirim = false;
    }

    public Notifikasi(int ID, int userID, String pesan, LocalDateTime waktu) {
        this(ID, userID, pesan);
        this.waktu = waktu;
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

    public boolean isTerkirim() {
        return terkirim;
    }
    
    public void setTerkirim() {
        this.terkirim = true;
    }

}
