package com.owo.utils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.owo.dao.AkunDAO;
import com.owo.dao.TiketDAO;

/**
 * Seeds the database with sample flights, hotels and a demo account.
 *
 * <p>Safe to run repeatedly: every insert is guarded by a natural-key lookup, so
 * re-running leaves the row counts unchanged rather than duplicating the seed set.
 *
 * <p>All dates are generated relative to today. Hardcoded dates are what made the
 * previous seed data expire; do not replace these with fixed dates.
 */
public class TestDataInserter {

    /** Credentials for the seeded demo account. Also documented in the README. */
    public static final String DEMO_EMAIL = "demo@owo.id";
    public static final String DEMO_PASSWORD = "demo1234";
    public static final String DEMO_NAMA = "Demo Pengguna";

    public static void insertSampleData() throws SQLException {
        System.out.println("Inserting sample test data...");

        // Initialize database
        DBHelper.initializeDatabase();

        insertDemoAccount();

        // Insert sample flight tickets
        insertSampleFlights();

        // Insert sample hotel tickets
        insertSampleHotels();

        System.out.println("Sample data insertion completed!");
    }

    private static void insertDemoAccount() throws SQLException {
        System.out.println("Inserting demo account...");

        if (AkunDAO.getAkunByEmail(DEMO_EMAIL) != null) {
            System.out.println("Demo account already present, skipping.");
            return;
        }

        AkunDAO.createAkun(DEMO_NAMA, DEMO_EMAIL, DEMO_PASSWORD);
        System.out.println("Demo account created: " + DEMO_EMAIL + " / " + DEMO_PASSWORD);
    }

    /** A departure that many days from now, at the given wall-clock time. */
    private static LocalDateTime departureIn(int days, int hour, int minute) {
        return LocalDate.now().plusDays(days).atTime(LocalTime.of(hour, minute));
    }

    private static void insertSampleFlights() throws SQLException {
        System.out.println("Inserting sample flights...");

        // Jakarta to Denpasar flights
        seedFlight(1500000f, "GA401",
                "Jakarta (CGK)", "Denpasar (DPS)",
                "Garuda Indonesia", "Ekonomi",
                departureIn(3, 7, 30));

        seedFlight(2500000f, "GA403",
                "Jakarta (CGK)", "Denpasar (DPS)",
                "Garuda Indonesia", "Bisnis",
                departureIn(3, 14, 15));

        seedFlight(1200000f, "JT750",
                "Jakarta (CGK)", "Denpasar (DPS)",
                "Lion Air", "Ekonomi",
                departureIn(3, 10, 45));

        // Denpasar to Jakarta flights (return)
        seedFlight(1600000f, "GA412",
                "Denpasar (DPS)", "Jakarta (CGK)",
                "Garuda Indonesia", "Ekonomi",
                departureIn(10, 16, 20));

        // Jakarta to Yogyakarta flights
        seedFlight(800000f, "QG701",
                "Jakarta (CGK)", "Yogyakarta (JOG)",
                "Citilink", "Ekonomi",
                departureIn(5, 8, 15));

        // Jakarta to Surabaya flights
        seedFlight(900000f, "GA305",
                "Jakarta (CGK)", "Surabaya (SBY)",
                "Garuda Indonesia", "Ekonomi",
                departureIn(7, 13, 45));

        System.out.println("Sample flights inserted successfully!");
    }

    private static void seedFlight(float harga, String flightNumber, String origin, String destination,
            String maskapai, String kelas, LocalDateTime waktuKeberangkatan) throws SQLException {
        if (TiketDAO.flightExists(flightNumber)) {
            return;
        }
        TiketDAO.createTiketPesawat(harga, true, flightNumber, origin, destination,
                maskapai, kelas, waktuKeberangkatan);
    }

    private static void insertSampleHotels() throws SQLException {
        System.out.println("Inserting sample hotels...");

        // Hotels in Denpasar
        seedHotel(750000f,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(8),
                "Nandini Jungle Resort & Spa", "101",
                "Jl. Raya Susut, Banjar Susut Kaja, Denpasar, Bali");

        seedHotel(1200000f,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(8),
                "Ramayana Suites & Resort", "204",
                "Jl. Bakung Sari, Kuta, Denpasar, Bali");

        seedHotel(950000f,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(8),
                "The Garcia Ubud Hotel & Resort", "305",
                "Jl. Raya Ubud No.88, Ubud, Denpasar, Bali");

        // Hotels in Jakarta
        seedHotel(650000f,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5),
                "Hotel Indonesia Kempinski", "1205",
                "Jl. M.H. Thamrin No.1, Menteng, Jakarta Pusat");

        seedHotel(850000f,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5),
                "The Ritz-Carlton Jakarta", "2108",
                "Jl. DR. Ide Anak Agung Gde Agung, Kuningan, Jakarta Selatan");

        // Hotels in Yogyakarta
        seedHotel(450000f,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(8),
                "Royal Malioboro Hotel", "308",
                "Jl. Malioboro No.18, Yogyakarta");

        // Hotels in Surabaya
        seedHotel(550000f,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(11),
                "Hotel Majapahit Surabaya", "412",
                "Jl. Tunjungan No.65, Genteng, Surabaya");

        // Hotels in Malang
        seedHotel(350000f,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(10),
                "Tugu Malang Hotel", "205",
                "Jl. Tugu No.3, Klojen, Malang");

        System.out.println("Sample hotels inserted successfully!");
    }

    private static void seedHotel(float harga, LocalDate checkIn, LocalDate checkOut,
            String hotelName, String roomNumber, String address) throws SQLException {
        if (TiketDAO.hotelExists(hotelName, roomNumber)) {
            return;
        }
        TiketDAO.createTiketHotel(harga, true, checkIn, checkOut, hotelName, roomNumber, address);
    }

    public static void main(String[] args) {
        try {
            insertSampleData();
        } catch (SQLException e) {
            System.err.println("Error inserting sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
