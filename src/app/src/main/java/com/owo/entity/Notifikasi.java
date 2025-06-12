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

    public Notifikasi(int ID, int userID, String pesan) {
        this.ID = ID;
        this.userID = userID;
        this.pesan = pesan;
        this.waktu = LocalDateTime.now();
        this.terkirm = false;
        allNotifikasi.add(this);
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

    public boolean isTerkirm() {
        return terkirm;
    }
    
    public void setTerkirm() {
        this.terkirm = true;
    }

    public static Notifikasi getNotifikasiByID(int ID) {
        for (Notifikasi notifikasi : allNotifikasi) {
            if (notifikasi.getID() == ID) {
                return notifikasi;
            }
        }
        return null;
    }
}
