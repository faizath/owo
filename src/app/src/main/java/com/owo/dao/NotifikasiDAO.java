package com.owo.dao;

import com.owo.entity.Notifikasi;
import com.owo.utils.DBHelper;
import com.owo.utils.SqlDates;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotifikasiDAO {
    public static Notifikasi createNotifikasi(int userID, String pesan) throws SQLException {
        String sql = "INSERT INTO notifikasi (user_id, pesan, waktu, terkirm) VALUES (?, ?, ?, 0)";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, userID);
            pstmt.setString(2, pesan);
            pstmt.setString(3, SqlDates.format(LocalDateTime.now()));
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating notifikasi failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return new Notifikasi(id, userID, pesan);
                } else {
                    throw new SQLException("Creating notifikasi failed, no ID obtained.");
                }
            }
        }
    }

    public static Notifikasi getNotifikasiById(int id) throws SQLException {
        String sql = "SELECT * FROM notifikasi WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int userID = rs.getInt("user_id");
                    String pesan = rs.getString("pesan");
                    LocalDateTime waktu = SqlDates.parseDateTime(rs.getString("waktu"), "waktu");
                    boolean terkirm = rs.getInt("terkirm") == 1;
                    
                    Notifikasi notifikasi = new Notifikasi(id, userID, pesan, waktu);
                    if (terkirm) {
                        notifikasi.setTerkirm();
                    }
                    return notifikasi;
                }
            }
        }
        return null;
    }

    public static List<Notifikasi> getNotifikasiByUserId(int userID) throws SQLException {
        String sql = "SELECT * FROM notifikasi WHERE user_id = ? ORDER BY waktu DESC";
        List<Notifikasi> notifikasiList = new ArrayList<>();
        
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String pesan = rs.getString("pesan");
                    LocalDateTime waktu = SqlDates.parseDateTime(rs.getString("waktu"), "waktu");
                    boolean terkirm = rs.getInt("terkirm") == 1;
                    
                    Notifikasi notifikasi = new Notifikasi(id, userID, pesan, waktu);
                    if (terkirm) {
                        notifikasi.setTerkirm();
                    }
                    notifikasiList.add(notifikasi);
                }
            }
        }
        return notifikasiList;
    }

    public static void markAsTerkirm(int id) throws SQLException {
        String sql = "UPDATE notifikasi SET terkirm = 1 WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
} 