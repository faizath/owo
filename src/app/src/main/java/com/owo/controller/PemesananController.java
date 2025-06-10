package com.owo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.owo.entity.Pemesanan;
import com.owo.entity.Tiket;

public class PemesananController {
    private Map<Integer, Pemesanan> pemesananMap;
    private int nextId;

    public PemesananController() {
        this.pemesananMap = new HashMap<>();
        this.nextId = 1;
    }

    public Pemesanan createPemesanan(String customerId, Tiket tiket) throws Exception {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new Exception("Customer ID tidak boleh kosong.");
        }
        if (tiket == null) {
            throw new Exception("Tiket tidak boleh kosong.");
        }
        
        Pemesanan pemesanan = new Pemesanan(nextId++, customerId, tiket);
        pemesananMap.put(pemesanan.getId(), pemesanan);
        return pemesanan;
    }

    public Pemesanan getPemesananById(int id) throws Exception {
        Pemesanan pemesanan = pemesananMap.get(id);
        if (pemesanan == null) {
            throw new Exception("Pemesanan dengan ID " + id + " tidak ditemukan.");
        }
        return pemesanan;
    }

    public void konfirmasiPemesanan(int pemesananId) throws Exception {
        Pemesanan pemesanan = getPemesananById(pemesananId);
        if (!pemesanan.getStatus().equals("PENDING")) {
            throw new Exception("Hanya pemesanan dengan status PENDING yang bisa dikonfirmasi.");
        }
        pemesanan.setStatus("CONFIRMED");
    }

    public void batalkanPemesanan(int pemesananId) throws Exception {
        Pemesanan pemesanan = getPemesananById(pemesananId);
        if (pemesanan.getStatus().equals("CANCELLED")) {
            throw new Exception("Pemesanan sudah dibatalkan sebelumnya.");
        }
        pemesanan.setStatus("CANCELLED");
    }
    
    public Pemesanan updateTiketPemesanan(int pemesananId, Tiket newTiket) throws Exception {
        Pemesanan pemesanan = getPemesananById(pemesananId);
        if (!pemesanan.getStatus().equals("PENDING")) {
            throw new Exception("Tiket hanya bisa diubah untuk pemesanan yang berstatus PENDING.");
        }
        if (newTiket == null) {
            throw new Exception("Tiket baru tidak boleh kosong.");
        }
        pemesanan.setTiket(newTiket);
        return pemesanan;
    }

    public List<Pemesanan> getAllPemesanan() {
        return new ArrayList<>(pemesananMap.values());
    }

    public List<Pemesanan> getPemesananByStatus(String status) {
        return pemesananMap.values().stream()
                .filter(p -> p.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    public List<Pemesanan> getPemesananByCustomerId(String customerId) {
        return pemesananMap.values().stream()
                .filter(p -> p.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    public void deletePemesanan(int pemesananId) throws Exception {
        if (!pemesananMap.containsKey(pemesananId)) {
            throw new Exception("Pemesanan dengan ID " + pemesananId + " tidak ditemukan untuk dihapus.");
        }
        pemesananMap.remove(pemesananId);
    }
}
