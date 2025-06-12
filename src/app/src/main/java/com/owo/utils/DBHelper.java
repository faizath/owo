package com.owo.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBHelper {
    private static final String DB_URL = "jdbc:sqlite:owo.db";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    public static void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // Create akun table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS akun (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nama TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    hashed_password TEXT NOT NULL
                )
            """);

            // Create tiket table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tiket (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    harga REAL NOT NULL,
                    tersedia INTEGER NOT NULL,
                    tipe TEXT NOT NULL
                )
            """);

            // Create tiket_pesawat table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tiket_pesawat (
                    tiket_id INTEGER PRIMARY KEY,
                    flight_number TEXT NOT NULL,
                    maskapai TEXT NOT NULL,
                    origin TEXT NOT NULL,
                    destination TEXT NOT NULL,
                    kelas TEXT NOT NULL,
                    waktu_keberangkatan TEXT NOT NULL,
                    FOREIGN KEY (tiket_id) REFERENCES tiket(id)
                )
            """);

            // Create tiket_hotel table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tiket_hotel (
                    tiket_id INTEGER PRIMARY KEY,
                    check_in TEXT NOT NULL,
                    check_out TEXT NOT NULL,
                    hotel_name TEXT NOT NULL,
                    room_number TEXT NOT NULL,
                    address TEXT NOT NULL,
                    FOREIGN KEY (tiket_id) REFERENCES tiket(id)
                )
            """);

            // Create pemesanan table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pemesanan (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    tiket_id INTEGER NOT NULL,
                    tanggal_pesan TEXT NOT NULL,
                    status TEXT NOT NULL,
                    FOREIGN KEY (customer_id) REFERENCES akun(id),
                    FOREIGN KEY (tiket_id) REFERENCES tiket(id)
                )
            """);

            // Create refund table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS refund (
                    id TEXT PRIMARY KEY,
                    pemesanan_id INTEGER NOT NULL,
                    alasan TEXT NOT NULL,
                    status TEXT NOT NULL,
                    jumlah_refund REAL NOT NULL,
                    nama_kartu TEXT,
                    nomor_kartu TEXT,
                    expiry_month TEXT,
                    expiry_year TEXT,
                    cvv TEXT,
                    FOREIGN KEY (pemesanan_id) REFERENCES pemesanan(id)
                )
            """);

            // Create notifikasi table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifikasi (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    pesan TEXT NOT NULL,
                    waktu TEXT NOT NULL,
                    terkirm INTEGER NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES akun(id)
                )
            """);
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
} 