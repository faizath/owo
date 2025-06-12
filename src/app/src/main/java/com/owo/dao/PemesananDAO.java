package com.owo.dao;

import com.owo.entity.Pemesanan;
import com.owo.entity.Tiket;
import com.owo.utils.DBHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PemesananDAO {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static Pemesanan createPemesanan(int customerId, Tiket tiket) throws SQLException {
        String sql = "INSERT INTO pemesanan (customer_id, tiket_id, tanggal_pesan, status) VALUES (?, ?, ?, 'PENDING')";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, customerId);
            pstmt.setInt(2, tiket.getId());
            pstmt.setString(3, LocalDateTime.now().format(DATETIME_FORMATTER));
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating pemesanan failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return new Pemesanan(id, String.valueOf(customerId), tiket);
                } else {
                    throw new SQLException("Creating pemesanan failed, no ID obtained.");
                }
            }
        }
    }

    public static Pemesanan getPemesananById(int id) throws SQLException {
        String sql = "SELECT p.*, t.* FROM pemesanan p " +
                    "JOIN tiket t ON p.tiket_id = t.id " +
                    "WHERE p.id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String customerId = rs.getString("customer_id");
                    int tiketId = rs.getInt("tiket_id");
                    LocalDateTime tanggalPesan = LocalDateTime.parse(rs.getString("tanggal_pesan"), DATETIME_FORMATTER);
                    String status = rs.getString("status");
                    
                    Tiket tiket = TiketDAO.getTiketById(tiketId);
                    Pemesanan pemesanan = new Pemesanan(id, customerId, tiket);
                    pemesanan.setTanggalPesan(tanggalPesan);
                    pemesanan.setStatus(status);
                    return pemesanan;
                }
            }
        }
        return null;
    }

    public static List<Pemesanan> getPemesananByCustomerId(int customerId) throws SQLException {
        String sql = "SELECT p.*, t.* FROM pemesanan p " +
                    "JOIN tiket t ON p.tiket_id = t.id " +
                    "WHERE p.customer_id = ?";
        List<Pemesanan> pemesananList = new ArrayList<>();
        
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int tiketId = rs.getInt("tiket_id");
                    LocalDateTime tanggalPesan = LocalDateTime.parse(rs.getString("tanggal_pesan"), DATETIME_FORMATTER);
                    String status = rs.getString("status");
                    
                    Tiket tiket = TiketDAO.getTiketById(tiketId);
                    Pemesanan pemesanan = new Pemesanan(id, String.valueOf(customerId), tiket);
                    pemesanan.setTanggalPesan(tanggalPesan);
                    pemesanan.setStatus(status);
                    pemesananList.add(pemesanan);
                }
            }
        }
        return pemesananList;
    }

    public static void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE pemesanan SET status = ? WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }
} 