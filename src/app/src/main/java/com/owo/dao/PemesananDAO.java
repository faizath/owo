package com.owo.dao;

import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.Tiket;
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

public class PemesananDAO {
    /**
     * Books an existing ticket, claiming its availability in the same transaction.
     *
     * <p>Availability is claimed with a conditional update, so two concurrent bookings of
     * the same ticket cannot both succeed. Nothing used to decrement availability at all,
     * which allowed the same seat or room to be sold without limit.
     *
     * @throws SQLException if the ticket is no longer available
     */
    public static Pemesanan createPemesanan(int customerId, Tiket tiket) throws SQLException {
        String claimSql = "UPDATE tiket SET tersedia = 0 WHERE id = ? AND tersedia = 1";
        String insertSql = "INSERT INTO pemesanan (customer_id, tiket_id, tanggal_pesan, status) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement claim = conn.prepareStatement(claimSql)) {
                    claim.setInt(1, tiket.getId());
                    if (claim.executeUpdate() == 0) {
                        throw new SQLException("Tiket " + tiket.getId() + " is no longer available");
                    }
                }

                try (PreparedStatement pstmt =
                             conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, customerId);
                    pstmt.setInt(2, tiket.getId());
                    pstmt.setString(3, SqlDates.format(LocalDateTime.now()));
                    pstmt.setString(4, PemesananStatus.PENDING.dbValue());

                    if (pstmt.executeUpdate() == 0) {
                        throw new SQLException("Creating pemesanan failed, no rows affected.");
                    }

                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new SQLException("Creating pemesanan failed, no ID obtained.");
                        }
                        conn.commit();
                        tiket.setTersedia(false);
                        return new Pemesanan(generatedKeys.getInt(1),
                                String.valueOf(customerId), tiket);
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Returns a claimed ticket to the catalogue. Used when a booking is cancelled. */
    public static void releaseTiket(int tiketId) throws SQLException {
        String sql = "UPDATE tiket SET tersedia = 1 WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tiketId);
            pstmt.executeUpdate();
        }
    }

    public static Pemesanan getPemesananById(int id) throws SQLException {
        // Both tables have an id column; alias explicitly rather than relying on
        // findColumn picking the "first" match, which is driver-dependent.
        String sql = "SELECT p.id AS pemesanan_id, p.customer_id, p.tiket_id, "
                + "p.tanggal_pesan, p.status FROM pemesanan p "
                + "JOIN tiket t ON p.tiket_id = t.id "
                + "WHERE p.id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String customerId = rs.getString("customer_id");
                    int tiketId = rs.getInt("tiket_id");
                    LocalDateTime tanggalPesan = SqlDates.parseDateTime(rs.getString("tanggal_pesan"), "tanggal_pesan");
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
        String sql = "SELECT p.id AS pemesanan_id, p.customer_id, p.tiket_id, "
                + "p.tanggal_pesan, p.status FROM pemesanan p "
                + "JOIN tiket t ON p.tiket_id = t.id "
                + "WHERE p.customer_id = ?";
        List<Pemesanan> pemesananList = new ArrayList<>();
        
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("pemesanan_id");
                    int tiketId = rs.getInt("tiket_id");
                    LocalDateTime tanggalPesan = SqlDates.parseDateTime(rs.getString("tanggal_pesan"), "tanggal_pesan");
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

    /**
     * @throws SQLException if no such booking exists. An update affecting zero rows used to
     *     be reported to the caller as success.
     */
    public static void updateStatus(int id, PemesananStatus status) throws SQLException {
        String sql = "UPDATE pemesanan SET status = ? WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.dbValue());
            pstmt.setInt(2, id);
            if (pstmt.executeUpdate() == 0) {
                throw new SQLException("No pemesanan with id " + id);
            }
        }
    }

    /** Reads the owning customer id without loading the whole booking. */
    public static Integer getOwnerId(int pemesananId) throws SQLException {
        String sql = "SELECT customer_id FROM pemesanan WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, pemesananId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt("customer_id") : null;
            }
        }
    }
} 