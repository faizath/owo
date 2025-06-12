package com.owo.dao;

import com.owo.entity.Tiket;
import com.owo.entity.TiketPesawat;
import com.owo.entity.TiketHotel;
import com.owo.utils.DBHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class TiketDAO {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static TiketPesawat createTiketPesawat(float harga, boolean tersedia, String flightNumber, 
            String origin, String destination, String maskapai, String kelas, LocalDateTime waktuKeberangkatan) throws SQLException {
        Connection conn = DBHelper.getConnection();
        try {
            conn.setAutoCommit(false);
            
            // Insert into base tiket table
            String tiketSql = "INSERT INTO tiket (harga, tersedia, tipe) VALUES (?, ?, 'PESAWAT')";
            try (PreparedStatement pstmt = conn.prepareStatement(tiketSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setFloat(1, harga);
                pstmt.setInt(2, tersedia ? 1 : 0);
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating tiket failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int tiketId = generatedKeys.getInt(1);
                        
                        // Insert into tiket_pesawat table
                        String pesawatSql = "INSERT INTO tiket_pesawat (tiket_id, flight_number, maskapai, origin, destination, kelas, waktu_keberangkatan) VALUES (?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement pesawatStmt = conn.prepareStatement(pesawatSql)) {
                            pesawatStmt.setInt(1, tiketId);
                            pesawatStmt.setString(2, flightNumber);
                            pesawatStmt.setString(3, maskapai);
                            pesawatStmt.setString(4, origin);
                            pesawatStmt.setString(5, destination);
                            pesawatStmt.setString(6, kelas);
                            pesawatStmt.setString(7, waktuKeberangkatan.format(DATETIME_FORMATTER));
                            
                            pesawatStmt.executeUpdate();
                        }
                        
                        conn.commit();
                        return new TiketPesawat(tiketId, harga, tersedia, flightNumber, origin, destination, 
                                maskapai, kelas, waktuKeberangkatan);
                    } else {
                        throw new SQLException("Creating tiket failed, no ID obtained.");
                    }
                }
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static TiketHotel createTiketHotel(float harga, boolean tersedia, LocalDate checkIn, 
            LocalDate checkOut, String hotelName, String roomNumber, String address) throws SQLException {
        Connection conn = DBHelper.getConnection();
        try {
            conn.setAutoCommit(false);
            
            // Insert into base tiket table
            String tiketSql = "INSERT INTO tiket (harga, tersedia, tipe) VALUES (?, ?, 'HOTEL')";
            try (PreparedStatement pstmt = conn.prepareStatement(tiketSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setFloat(1, harga);
                pstmt.setInt(2, tersedia ? 1 : 0);
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating tiket failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int tiketId = generatedKeys.getInt(1);
                        
                        // Insert into tiket_hotel table
                        String hotelSql = "INSERT INTO tiket_hotel (tiket_id, check_in, check_out, hotel_name, room_number, address) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement hotelStmt = conn.prepareStatement(hotelSql)) {
                            hotelStmt.setInt(1, tiketId);
                            hotelStmt.setString(2, checkIn.format(DATE_FORMATTER));
                            hotelStmt.setString(3, checkOut.format(DATE_FORMATTER));
                            hotelStmt.setString(4, hotelName);
                            hotelStmt.setString(5, roomNumber);
                            hotelStmt.setString(6, address);
                            
                            hotelStmt.executeUpdate();
                        }
                        
                        conn.commit();
                        return new TiketHotel(tiketId, harga, tersedia, checkIn, checkOut, 
                                hotelName, roomNumber, address);
                    } else {
                        throw new SQLException("Creating tiket failed, no ID obtained.");
                    }
                }
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
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
                        LocalDateTime waktuKeberangkatan = LocalDateTime.parse(
                            rs.getString("waktu_keberangkatan"), DATETIME_FORMATTER);
                        
                        return new TiketPesawat(id, harga, tersedia, flightNumber, origin, 
                                destination, maskapai, kelas, waktuKeberangkatan);
                    } else if ("HOTEL".equals(tipe)) {
                        LocalDate checkIn = LocalDate.parse(rs.getString("check_in"), DATE_FORMATTER);
                        LocalDate checkOut = LocalDate.parse(rs.getString("check_out"), DATE_FORMATTER);
                        String hotelName = rs.getString("hotel_name");
                        String roomNumber = rs.getString("room_number");
                        String address = rs.getString("address");
                        
                        return new TiketHotel(id, harga, tersedia, checkIn, checkOut, 
                                hotelName, roomNumber, address);
                    }
                }
            }
        }
        return null;
    }

    public static void updateTiketAvailability(int id, boolean tersedia) throws SQLException {
        String sql = "UPDATE tiket SET tersedia = ? WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, tersedia ? 1 : 0);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Search for flight tickets with filters
     * @param origin Flight origin (null to ignore)
     * @param destination Flight destination (null to ignore)
     * @param kelas Flight class (null to ignore)
     * @param passengers Number of passengers (0 to ignore)
     * @param tersediaOnly If true, only return available tickets
     * @return List of matching flight tickets
     */
    public static List<TiketPesawat> searchTiketPesawat(String origin, String destination, 
            String kelas, int passengers, boolean tersediaOnly) throws SQLException {
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
        
        // For passengers, we currently don't have seat capacity in the schema
        // This is a placeholder for future implementation
        if (passengers > 0) {
            // Could add seat availability check here in the future
            // For now, we'll just include it as a comment for extensibility
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
                    LocalDateTime waktuKeberangkatan = LocalDateTime.parse(
                        rs.getString("waktu_keberangkatan"), DATETIME_FORMATTER);
                    
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
     * @param guests Number of guests (0 to ignore)
     * @param tersediaOnly If true, only return available tickets
     * @return List of matching hotel tickets
     */
    public static List<TiketHotel> searchTiketHotel(String location, LocalDate checkIn, 
            LocalDate checkOut, String hotelName, int guests, boolean tersediaOnly) throws SQLException {
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
        
        if (checkIn != null) {
            sqlBuilder.append(" AND th.check_in >= ?");
            parameters.add(checkIn.format(DATE_FORMATTER));
        }
        
        if (checkOut != null) {
            sqlBuilder.append(" AND th.check_out <= ?");
            parameters.add(checkOut.format(DATE_FORMATTER));
        }
        
        if (tersediaOnly) {
            sqlBuilder.append(" AND t.tersedia = 1");
        }
        
        // For guests, we currently don't have room capacity in the schema
        // This is a placeholder for future implementation
        if (guests > 0) {
            // Could add room capacity check here in the future
            // For now, we'll just include it as a comment for extensibility
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
                    LocalDate rsCheckIn = LocalDate.parse(rs.getString("check_in"), DATE_FORMATTER);
                    LocalDate rsCheckOut = LocalDate.parse(rs.getString("check_out"), DATE_FORMATTER);
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
        return searchTiketPesawat(null, null, null, 0, true);
    }

    /**
     * Get all available hotel tickets (shortcut method)
     */
    public static List<TiketHotel> getAllAvailableHotels() throws SQLException {
        return searchTiketHotel(null, null, null, null, 0, true);
    }
} 