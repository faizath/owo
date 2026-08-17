package com.owo.dao;

import com.owo.entity.Tiket;
import com.owo.entity.TiketPesawat;
import com.owo.entity.TiketHotel;
import com.owo.utils.DBHelper;
import com.owo.utils.SqlDates;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Ticket persistence and search.
 *
 * <p>Search takes no passenger or guest count. The schema models one ticket as one
 * bookable unit and has no capacity column, so those arguments used to be accepted and
 * then silently dropped into an empty {@code if} block — a filter the caller believed was
 * applied. The UI still asks how many people are travelling, for the booking summary; it
 * is not a search filter until capacity is modelled.
 */
public class TiketDAO {
    public static TiketPesawat createTiketPesawat(float harga, boolean tersedia, String flightNumber,
            String origin, String destination, String maskapai, String kelas, LocalDateTime waktuKeberangkatan)
            throws SQLException {
        String tiketSql = "INSERT INTO tiket (harga, tersedia, tipe) VALUES (?, ?, 'PESAWAT')";
        String pesawatSql = "INSERT INTO tiket_pesawat (tiket_id, flight_number, maskapai, origin, "
                + "destination, kelas, waktu_keberangkatan) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int tiketId = insertTiket(conn, tiketSql, harga, tersedia);

                try (PreparedStatement pesawatStmt = conn.prepareStatement(pesawatSql)) {
                    pesawatStmt.setInt(1, tiketId);
                    pesawatStmt.setString(2, flightNumber);
                    pesawatStmt.setString(3, maskapai);
                    pesawatStmt.setString(4, origin);
                    pesawatStmt.setString(5, destination);
                    pesawatStmt.setString(6, kelas);
                    pesawatStmt.setString(7, SqlDates.format(waktuKeberangkatan));

                    pesawatStmt.executeUpdate();
                }

                conn.commit();
                return new TiketPesawat(tiketId, harga, tersedia, flightNumber, origin, destination,
                        maskapai, kelas, waktuKeberangkatan);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static TiketHotel createTiketHotel(float harga, boolean tersedia, LocalDate checkIn,
            LocalDate checkOut, String hotelName, String roomNumber, String address) throws SQLException {
        String tiketSql = "INSERT INTO tiket (harga, tersedia, tipe) VALUES (?, ?, 'HOTEL')";
        String hotelSql = "INSERT INTO tiket_hotel (tiket_id, check_in, check_out, hotel_name, "
                + "room_number, address) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int tiketId = insertTiket(conn, tiketSql, harga, tersedia);

                try (PreparedStatement hotelStmt = conn.prepareStatement(hotelSql)) {
                    hotelStmt.setInt(1, tiketId);
                    hotelStmt.setString(2, SqlDates.format(checkIn));
                    hotelStmt.setString(3, SqlDates.format(checkOut));
                    hotelStmt.setString(4, hotelName);
                    hotelStmt.setString(5, roomNumber);
                    hotelStmt.setString(6, address);

                    hotelStmt.executeUpdate();
                }

                conn.commit();
                return new TiketHotel(tiketId, harga, tersedia, checkIn, checkOut,
                        hotelName, roomNumber, address);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Inserts the base row and returns its generated id. Runs inside the caller's transaction. */
    private static int insertTiket(Connection conn, String sql, float harga, boolean tersedia)
            throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setFloat(1, harga);
            pstmt.setInt(2, tersedia ? 1 : 0);

            if (pstmt.executeUpdate() == 0) {
                throw new SQLException("Creating tiket failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating tiket failed, no ID obtained.");
                }
                return generatedKeys.getInt(1);
            }
        }
    }

    public static Tiket getTiketById(int id) throws SQLException {
        String sql = "SELECT t.*, tp.*, th.* FROM tiket t " +
                    "LEFT JOIN tiket_pesawat tp ON t.id = tp.tiket_id " +
                    "LEFT JOIN tiket_hotel th ON t.id = th.tiket_id " +
                    "WHERE t.id = ?";
                    
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String tipe = rs.getString("tipe");
                    float harga = rs.getFloat("harga");
                    boolean tersedia = rs.getInt("tersedia") == 1;

                    if ("PESAWAT".equals(tipe)) {
                        String flightNumber = rs.getString("flight_number");
                        String maskapai = rs.getString("maskapai");
                        String origin = rs.getString("origin");
                        String destination = rs.getString("destination");
                        String kelas = rs.getString("kelas");
                        LocalDateTime waktuKeberangkatan =
                            SqlDates.parseDateTime(rs.getString("waktu_keberangkatan"), "waktu_keberangkatan");

                        return new TiketPesawat(id, harga, tersedia, flightNumber, origin,
                                destination, maskapai, kelas, waktuKeberangkatan);
                    } else if ("HOTEL".equals(tipe)) {
                        LocalDate checkIn = SqlDates.parseDate(rs.getString("check_in"), "check_in");
                        LocalDate checkOut = SqlDates.parseDate(rs.getString("check_out"), "check_out");
                        String hotelName = rs.getString("hotel_name");
                        String roomNumber = rs.getString("room_number");
                        String address = rs.getString("address");

                        return new TiketHotel(id, harga, tersedia, checkIn, checkOut,
                                hotelName, roomNumber, address);
                    }
                    // A row exists but carries a tipe this code cannot map. Returning null here
                    // used to surface much later as an NPE on Pemesanan.getTiket().
                    throw new SQLException("Tiket " + id + " has unrecognised tipe: " + tipe);
                }
            }
        }
        return null;
    }

    /**
     * Natural-key lookup used by the seeder to stay idempotent. Flight numbers are
     * unique within the seed set, so this is enough to detect an already-seeded row.
     */
    public static boolean flightExists(String flightNumber) throws SQLException {
        String sql = "SELECT 1 FROM tiket_pesawat WHERE flight_number = ? LIMIT 1";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, flightNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Natural-key lookup used by the seeder to stay idempotent. A hotel may appear
     * more than once, so the room number is part of the key.
     */
    public static boolean hotelExists(String hotelName, String roomNumber) throws SQLException {
        String sql = "SELECT 1 FROM tiket_hotel WHERE hotel_name = ? AND room_number = ? LIMIT 1";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, hotelName);
            pstmt.setString(2, roomNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }


    /**
     * Search for flight tickets with filters
     * @param origin Flight origin (null to ignore)
     * @param destination Flight destination (null to ignore)
     * @param kelas Flight class (null to ignore)
     * @param tersediaOnly If true, only return available tickets
     * @return List of matching flight tickets
     */
    public static List<TiketPesawat> searchTiketPesawat(String origin, String destination,
            String kelas, boolean tersediaOnly) throws SQLException {
        List<TiketPesawat> results = new ArrayList<>();
        
        // Build dynamic SQL query
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT t.*, tp.* FROM tiket t ")
                  .append("INNER JOIN tiket_pesawat tp ON t.id = tp.tiket_id ")
                  .append("WHERE t.tipe = 'PESAWAT'");
        
        List<Object> parameters = new ArrayList<>();
        
        // Add filters
        if (origin != null && !origin.trim().isEmpty()) {
            sqlBuilder.append(" AND LOWER(tp.origin) LIKE LOWER(?)");
            parameters.add("%" + origin.trim() + "%");
        }
        
        if (destination != null && !destination.trim().isEmpty()) {
            sqlBuilder.append(" AND LOWER(tp.destination) LIKE LOWER(?)");
            parameters.add("%" + destination.trim() + "%");
        }
        
        if (kelas != null && !kelas.trim().isEmpty()) {
            sqlBuilder.append(" AND LOWER(tp.kelas) LIKE LOWER(?)");
            parameters.add("%" + kelas.trim() + "%");
        }
        
        if (tersediaOnly) {
            sqlBuilder.append(" AND t.tersedia = 1");
        }
        
        
        sqlBuilder.append(" ORDER BY tp.waktu_keberangkatan ASC, t.harga ASC");
        
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            
            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    float harga = rs.getFloat("harga");
                    boolean tersedia = rs.getInt("tersedia") == 1;
                    String flightNumber = rs.getString("flight_number");
                    String maskapai = rs.getString("maskapai");
                    String rsOrigin = rs.getString("origin");
                    String rsDestination = rs.getString("destination");
                    String rsKelas = rs.getString("kelas");
                    LocalDateTime waktuKeberangkatan =
                        SqlDates.parseDateTime(rs.getString("waktu_keberangkatan"), "waktu_keberangkatan");
                    
                    TiketPesawat tiket = new TiketPesawat(id, harga, tersedia, flightNumber, 
                            rsOrigin, rsDestination, maskapai, rsKelas, waktuKeberangkatan);
                    results.add(tiket);
                }
            }
        }
        
        return results;
    }

    /**
     * Search for hotel tickets with filters
     * @param location Hotel location/address (null to ignore)
     * @param checkIn Check-in date (null to ignore)
     * @param checkOut Check-out date (null to ignore)
     * @param hotelName Hotel name (null to ignore)
     * @param tersediaOnly If true, only return available tickets
     * @return List of matching hotel tickets
     */
    public static List<TiketHotel> searchTiketHotel(String location, LocalDate checkIn,
            LocalDate checkOut, String hotelName, boolean tersediaOnly) throws SQLException {
        List<TiketHotel> results = new ArrayList<>();
        
        // Build dynamic SQL query
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT t.*, th.* FROM tiket t ")
                  .append("INNER JOIN tiket_hotel th ON t.id = th.tiket_id ")
                  .append("WHERE t.tipe = 'HOTEL'");
        
        List<Object> parameters = new ArrayList<>();
        
        // Add filters
        if (location != null && !location.trim().isEmpty()) {
            sqlBuilder.append(" AND LOWER(th.address) LIKE LOWER(?)");
            parameters.add("%" + location.trim() + "%");
        }
        
        if (hotelName != null && !hotelName.trim().isEmpty()) {
            sqlBuilder.append(" AND LOWER(th.hotel_name) LIKE LOWER(?)");
            parameters.add("%" + hotelName.trim() + "%");
        }
        
        // The requested stay must fit inside the listing's window, not the other way round.
        if (checkIn != null) {
            sqlBuilder.append(" AND th.check_in <= ?");
            parameters.add(SqlDates.format(checkIn));
        }

        if (checkOut != null) {
            sqlBuilder.append(" AND th.check_out >= ?");
            parameters.add(SqlDates.format(checkOut));
        }
        
        if (tersediaOnly) {
            sqlBuilder.append(" AND t.tersedia = 1");
        }
        
        
        sqlBuilder.append(" ORDER BY th.check_in ASC, t.harga ASC");
        
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            
            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    float harga = rs.getFloat("harga");
                    boolean tersedia = rs.getInt("tersedia") == 1;
                    LocalDate rsCheckIn = SqlDates.parseDate(rs.getString("check_in"), "check_in");
                    LocalDate rsCheckOut = SqlDates.parseDate(rs.getString("check_out"), "check_out");
                    String rsHotelName = rs.getString("hotel_name");
                    String roomNumber = rs.getString("room_number");
                    String address = rs.getString("address");
                    
                    TiketHotel tiket = new TiketHotel(id, harga, tersedia, rsCheckIn, rsCheckOut, 
                            rsHotelName, roomNumber, address);
                    results.add(tiket);
                }
            }
        }
        
        return results;
    }

    /**
     * Get all available flight tickets (shortcut method)
     */
    public static List<TiketPesawat> getAllAvailableFlights() throws SQLException {
        return searchTiketPesawat(null, null, null, true);
    }

    /**
     * Get all available hotel tickets (shortcut method)
     */
    public static List<TiketHotel> getAllAvailableHotels() throws SQLException {
        return searchTiketHotel(null, null, null, null, true);
    }
} 