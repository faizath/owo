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
        return createPemesanan(customerId, tiket, 1);
    }

    /**
     * As {@link #createPemesanan(int, Tiket)}, for a stated party size.
     *
     * <p>The claim is conditional on capacity as well as availability, so a party larger
     * than the unit holds is rejected by the database rather than by a check the caller
     * could skip.
     *
     * @throws SQLException if the ticket is unavailable or too small for the party
     */
    public static Pemesanan createPemesanan(int customerId, Tiket tiket, int jumlahPeserta)
            throws SQLException {
        if (jumlahPeserta < 1) {
            throw new SQLException("Jumlah peserta minimal 1, bukan " + jumlahPeserta);
        }

        String claimSql =
                "UPDATE tiket SET tersedia = 0 WHERE id = ? AND tersedia = 1 AND kapasitas >= ?";
        String insertSql = "INSERT INTO pemesanan "
                + "(customer_id, tiket_id, tanggal_pesan, status, jumlah_peserta) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement claim = conn.prepareStatement(claimSql)) {
                    claim.setInt(1, tiket.getId());
                    claim.setInt(2, jumlahPeserta);
                    if (claim.executeUpdate() == 0) {
                        throw new SQLException("Tiket " + tiket.getId()
                                + " is no longer available, or cannot seat " + jumlahPeserta);
                    }
                }

                try (PreparedStatement pstmt =
                             conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, customerId);
                    pstmt.setInt(2, tiket.getId());
                    pstmt.setString(3, SqlDates.format(LocalDateTime.now()));
                    pstmt.setString(4, PemesananStatus.PENDING.dbValue());
                    pstmt.setInt(5, jumlahPeserta);

                    if (pstmt.executeUpdate() == 0) {
                        throw new SQLException("Creating pemesanan failed, no rows affected.");
                    }

                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new SQLException("Creating pemesanan failed, no ID obtained.");
                        }
                        conn.commit();
                        tiket.setTersedia(false);
                        Pemesanan pemesanan = new Pemesanan(generatedKeys.getInt(1),
                                String.valueOf(customerId), tiket);
                        pemesanan.setJumlahPeserta(jumlahPeserta);
                        return pemesanan;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Cancels a booking and returns its ticket to the catalogue as one transaction.
     *
     * <p>These were two writes on two connections. A failure between them left the booking
     * CANCELLED while its unit stayed claimed — and because CANCELLED is terminal, nothing
     * could ever release it again. That is the same permanent loss of a unit the cancel
     * button exists to prevent, so it must not be reachable by crashing halfway.
     *
     * <p>The status update is conditional on the status the caller validated against, so a
     * booking that changed underneath a slow request is refused rather than overwritten.
     *
     * @param tiketId the unit to release, or null when the booking has no ticket row
     * @return false if the booking was no longer in {@code from}
     */
    public static boolean cancelAndRelease(int pemesananId, PemesananStatus from, Integer tiketId)
            throws SQLException {
        String cancelSql = "UPDATE pemesanan SET status = ? WHERE id = ? AND status = ?";
        String releaseSql = "UPDATE tiket SET tersedia = 1 WHERE id = ?";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement cancel = conn.prepareStatement(cancelSql)) {
                    cancel.setString(1, PemesananStatus.CANCELLED.dbValue());
                    cancel.setInt(2, pemesananId);
                    cancel.setString(3, from.dbValue());
                    if (cancel.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                if (tiketId != null) {
                    try (PreparedStatement release = conn.prepareStatement(releaseSql)) {
                        release.setInt(1, tiketId);
                        release.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Returns a claimed ticket to the catalogue. */
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
                + "p.tanggal_pesan, p.status, p.jumlah_peserta FROM pemesanan p "
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
                    pemesanan.setJumlahPeserta(rs.getInt("jumlah_peserta"));
                    return pemesanan;
                }
            }
        }
        return null;
    }

    public static List<Pemesanan> getPemesananByCustomerId(int customerId) throws SQLException {
        String sql = "SELECT p.id AS pemesanan_id, p.customer_id, p.tiket_id, "
                + "p.tanggal_pesan, p.status, p.jumlah_peserta FROM pemesanan p "
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
                    
                    int jumlahPeserta = rs.getInt("jumlah_peserta");

                    Tiket tiket = TiketDAO.getTiketById(tiketId);
                    Pemesanan pemesanan = new Pemesanan(id, String.valueOf(customerId), tiket);
                    pemesanan.setTanggalPesan(tanggalPesan);
                    pemesanan.setStatus(status);
                    pemesanan.setJumlahPeserta(jumlahPeserta);
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