package com.owo.db;

import com.owo.entity.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SampleDatabase {

    public static final Map<Integer, Pemesanan> pemesananTable = new HashMap<>();
    public static final Map<String, Refund> refundTable = new HashMap<>();

    static {
        TiketPesawat tiketPesawat1 = new TiketPesawat(101, 1500000f, true, "GA-202", "CGK", "DPS", "Garuda Indonesia",
                LocalDateTime.now().plusDays(10));
        TiketHotel tiketHotel1 = new TiketHotel(201, 2500000f, true, LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(32), "Hotel Mewah Bali", "101", "Jl. Pantai Kuta No.1");
        TiketPesawat tiketPesawat2 = new TiketPesawat(102, 800000f, true, "QZ-7510", "SUB", "CGK", "AirAsia",
                LocalDateTime.now().plusDays(3)); // Tiket untuk direfund < 7 hari
        TiketPesawat tiketPesawat3 = new TiketPesawat(103, 900000f, true, "JT-582", "CGK", "KNO", "Lion Air",
                LocalDateTime.now().plusDays(15));

        Pemesanan p1 = new Pemesanan(1, "cust-01", tiketPesawat1);
        p1.setStatus("CONFIRMED");

        Pemesanan p2 = new Pemesanan(2, "cust-02", tiketHotel1);
        p2.setStatus("CONFIRMED");

        Pemesanan p3 = new Pemesanan(3, "cust-01", tiketPesawat2);
        p3.setStatus("CONFIRMED");

        Pemesanan p4 = new Pemesanan(4, "cust-03", tiketPesawat3);
        p4.setStatus("CANCELLED");

        pemesananTable.put(p1.getId(), p1);
        pemesananTable.put(p2.getId(), p2);
        pemesananTable.put(p3.getId(), p3);
        pemesananTable.put(p4.getId(), p4);
    }
}