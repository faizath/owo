package com.owo.dao;

import com.owo.entity.Refund;
import com.owo.utils.DBHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RefundDAO {

    public static Refund createRefund(String id, int pemesananID, String alasan, double jumlahRefund) throws SQLException {
        String sql = "INSERT INTO refund (id, pemesanan_id, alasan, status, jumlah_refund) VALUES (?, ?, ?, 'PENDING_REVIEW', ?)";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            pstmt.setInt(2, pemesananID);
            pstmt.setString(3, alasan);
            pstmt.setDouble(4, jumlahRefund);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating refund failed, no rows affected.");
            }

            return new Refund(id, pemesananID, alasan, jumlahRefund);
        }
    }

    public static Refund getRefundById(String id) throws SQLException {
        String sql = "SELECT * FROM refund WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int pemesananID = rs.getInt("pemesanan_id");
                    String alasan = rs.getString("alasan");
                    Refund.RefundStatus status = Refund.RefundStatus.valueOf(rs.getString("status"));
                    double jumlahRefund = rs.getDouble("jumlah_refund");
                    String namaKartu = rs.getString("nama_kartu");
                    String nomorKartu = rs.getString("nomor_kartu");
                    String expiryMonth = rs.getString("expiry_month");
                    String expiryYear = rs.getString("expiry_year");
                    String cvv = rs.getString("cvv");
                    
                    Refund refund = new Refund(id, pemesananID, alasan, jumlahRefund);
                    refund.setStatus(status);
                    if (namaKartu != null) {
                        refund.setDetailKartu(namaKartu, nomorKartu, expiryMonth, expiryYear, cvv);
                    }
                    return refund;
                }
            }
        }
        return null;
    }

    public static List<Refund> getRefundByPemesananId(int pemesananID) throws SQLException {
        String sql = "SELECT * FROM refund WHERE pemesanan_id = ?";
        List<Refund> refundList = new ArrayList<>();
        
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, pemesananID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String alasan = rs.getString("alasan");
                    Refund.RefundStatus status = Refund.RefundStatus.valueOf(rs.getString("status"));
                    double jumlahRefund = rs.getDouble("jumlah_refund");
                    String namaKartu = rs.getString("nama_kartu");
                    String nomorKartu = rs.getString("nomor_kartu");
                    String expiryMonth = rs.getString("expiry_month");
                    String expiryYear = rs.getString("expiry_year");
                    String cvv = rs.getString("cvv");
                    
                    Refund refund = new Refund(id, pemesananID, alasan, jumlahRefund);
                    refund.setStatus(status);
                    if (namaKartu != null) {
                        refund.setDetailKartu(namaKartu, nomorKartu, expiryMonth, expiryYear, cvv);
                    }
                    refundList.add(refund);
                }
            }
        }
        return refundList;
    }

    public static void updateStatus(String id, Refund.RefundStatus status) throws SQLException {
        String sql = "UPDATE refund SET status = ? WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.name());
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        }
    }

    public static void updateDetailKartu(String id, String namaKartu, String nomorKartu, 
            String expiryMonth, String expiryYear, String cvv) throws SQLException {
        String sql = "UPDATE refund SET nama_kartu = ?, nomor_kartu = ?, expiry_month = ?, expiry_year = ?, cvv = ? WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, namaKartu);
            pstmt.setString(2, nomorKartu);
            pstmt.setString(3, expiryMonth);
            pstmt.setString(4, expiryYear);
            pstmt.setString(5, cvv);
            pstmt.setString(6, id);
            pstmt.executeUpdate();
        }
    }
} 