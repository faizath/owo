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
 * <p>One {@code tiket} row is one bookable unit, and {@code kapasitas} says how many people
 * that unit holds. Search filters on it, so a party of four never sees a listing that seats
 * two. The count used to be accepted by these methods and dropped into an empty {@code if}
 * block — a filter the caller believed was applied.
 */
public class TiketDAO {
    /** Capacity assumed when a caller does not state one. */
    private static final int KAPASITAS_DEFAULT = 1;

    public static TiketPesawat createTiketPesawat(float harga, boolean tersedia, String flightNumber,
            String origin, String destination, String maskapai, String kelas, LocalDateTime waktuKeberangkatan)
            throws SQLException {
        return createTiketPesawat(harga, tersedia, flightNumber, origin, destination, maskapai,
                kelas, waktuKeberangkatan, KAPASITAS_DEFAULT);
    }

    public static TiketPesawat createTiketPesawat(float harga, boolean tersedia, String flightNumber,
            String origin, String destination, String maskapai, String kelas,
            LocalDateTime waktuKeberangkatan, int kapasitas)
            throws SQLException {
        String tiketSql =
                "INSERT INTO tiket (harga, tersedia, tipe, kapasitas) VALUES (?, ?, 'PESAWAT', ?)";
        String pesawatSql = "INSERT INTO tiket_pesawat (tiket_id, flight_number, maskapai, origin, "
                + "destination, kelas, waktu_keberangkatan) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int tiketId = insertTiket(conn, tiketSql, harga, tersedia, kapasitas);

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
                TiketPesawat tiket = TiketPesawat.builder()
                        .id(tiketId)
                        .harga(harga)
                        .tersedia(tersedia)
                        .flightNumber(flightNumber)
                        .origin(origin)
                        .destination(destination)
                        .maskapai(maskapai)
                        .kelas(kelas)
                        .waktuKeberangkatan(waktuKeberangkatan)
                        .build();
                tiket.setKapasitas(kapasitas);
                return tiket;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static TiketHotel createTiketHotel(float harga, boolean tersedia, LocalDate checkIn,
            LocalDate checkOut, String hotelName, String roomNumber, String address) throws SQLException {
        return createTiketHotel(harga, tersedia, checkIn, checkOut, hotelName, roomNumber, address,
                KAPASITAS_DEFAULT);
    }

    public static TiketHotel createTiketHotel(float harga, boolean tersedia, LocalDate checkIn,
            LocalDate checkOut, String hotelName, String roomNumber, String address, int kapasitas)
            throws SQLException {
        String tiketSql =
                "INSERT INTO tiket (harga, tersedia, tipe, kapasitas) VALUES (?, ?, 'HOTEL', ?)";
        String hotelSql = "INSERT INTO tiket_hotel (tiket_id, check_in, check_out, hotel_name, "
                + "room_number, address) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int tiketId = insertTiket(conn, tiketSql, harga, tersedia, kapasitas);

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
                TiketHotel tiket = new TiketHotel(tiketId, harga, tersedia, checkIn, checkOut,
                        hotelName, roomNumber, address);
                tiket.setKapasitas(kapasitas);
                return tiket;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Inserts the base row and returns its generated id. Runs inside the caller's transaction. */
    private static int insertTiket(Connection conn, String sql, float harga, boolean tersedia,
            int kapasitas) throws SQLException {
        if (kapasitas < 1) {
            throw new SQLException("Kapasitas tiket minimal 1, bukan " + kapasitas);
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setFloat(1, harga);
            pstmt.setInt(2, tersedia ? 1 : 0);
            pstmt.setInt(3, kapasitas);

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
                    int kapasitas = rs.getInt("kapasitas");

                    if ("PESAWAT".equals(tipe)) {
                        String flightNumber = rs.getString("flight_number");
                        String maskapai = rs.getString("maskapai");
                        String origin = rs.getString("origin");
                        String destination = rs.getString("destination");
                        String kelas = rs.getString("kelas");
                        LocalDateTime waktuKeberangkatan =
                            SqlDates.parseDateTime(rs.getString("waktu_keberangkatan"), "waktu_keberangkatan");

                        TiketPesawat pesawat = TiketPesawat.builder()
                        .id(id)
                        .harga(harga)
                        .tersedia(tersedia)
                        .flightNumber(flightNumber)
                        .origin(origin)
                        .destination(destination)
                        .maskapai(maskapai)
                        .kelas(kelas)
                        .waktuKeberangkatan(waktuKeberangkatan)
                        .build();
                        pesawat.setKapasitas(kapasitas);
                        return pesawat;
                    } else if ("HOTEL".equals(tipe)) {
                        LocalDate checkIn = SqlDates.parseDate(rs.getString("check_in"), "check_in");
                        LocalDate checkOut = SqlDates.parseDate(rs.getString("check_out"), "check_out");
                        String hotelName = rs.getString("hotel_name");
                        String roomNumber = rs.getString("room_number");
                        String address = rs.getString("address");

                        TiketHotel hotel = new TiketHotel(id, harga, tersedia, checkIn, checkOut,
                                hotelName, roomNumber, address);
                        hotel.setKapasitas(kapasitas);
                        return hotel;
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
        return searchTiketPesawat(origin, destination, kelas, tersediaOnly, KAPASITAS_DEFAULT);
    }

    /**
     * @param minKapasitas the party size that must fit; rows seating fewer are excluded.
     *     Results are ordered so that, among flights leaving at the same time, the
     *     smallest unit holding the party comes first.
     */
    public static List<TiketPesawat> searchTiketPesawat(String origin, String destination,
            String kelas, boolean tersediaOnly, int minKapasitas) throws SQLException {
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

        // Unconditional. Skipping it for a party of one made the javadoc a lie, and left
        // the guard reading as though a solo search were a special case when it is not.
        sqlBuilder.append(" AND t.kapasitas >= ?");
        parameters.add(minKapasitas);

        // Capacity ahead of price so that among equally timed flights the smallest unit
        // that fits the party is offered first. One booking claims a whole unit, so
        // showing a solo traveller a four-seat block first denies it to a party of four.
        sqlBuilder.append(" ORDER BY tp.waktu_keberangkatan ASC, t.kapasitas ASC, t.harga ASC");
        
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
                    
                    TiketPesawat tiket = TiketPesawat.builder()
                        .id(id)
                        .harga(harga)
                        .tersedia(tersedia)
                        .flightNumber(flightNumber)
                        .origin(rsOrigin)
                        .destination(rsDestination)
                        .maskapai(maskapai)
                        .kelas(rsKelas)
                        .waktuKeberangkatan(waktuKeberangkatan)
                        .build();
                    tiket.setKapasitas(rs.getInt("kapasitas"));
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
        return searchTiketHotel(location, checkIn, checkOut, hotelName, tersediaOnly,
                KAPASITAS_DEFAULT);
    }

    /**
     * @param minKapasitas the number of guests that must fit in one room; rooms holding
     *     fewer are excluded. Among rooms available from the same date, the smallest one
     *     that holds the party comes first.
     */
    public static List<TiketHotel> searchTiketHotel(String location, LocalDate checkIn,
            LocalDate checkOut, String hotelName, boolean tersediaOnly, int minKapasitas)
            throws SQLException {
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

        // Unconditional. Skipping it for a party of one made the javadoc a lie, and left
        // the guard reading as though a solo search were a special case when it is not.
        sqlBuilder.append(" AND t.kapasitas >= ?");
        parameters.add(minKapasitas);

        // As for flights: the smallest room that holds the party comes first, so a single
        // guest is not handed the family room while a family has nowhere to stay.
        sqlBuilder.append(" ORDER BY th.check_in ASC, t.kapasitas ASC, t.harga ASC");
        
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
                    tiket.setKapasitas(rs.getInt("kapasitas"));
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