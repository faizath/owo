package com.owo.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the database location and hands out connections.
 *
 * <p>Every call to {@link #getConnection()} returns a <em>new</em> connection. Callers
 * are expected to close it, which the DAOs already do via try-with-resources. Sharing
 * one static connection meant a nested DAO call closed the caller's result set out from
 * under it, and made {@code setAutoCommit(false)} a process-wide transaction.
 *
 * <p>The database path is resolved once, in this order:
 * <ol>
 *   <li>the {@code owo.db.path} system property,</li>
 *   <li>the {@code OWO_DB_PATH} environment variable,</li>
 *   <li>{@code ~/.owo/owo.db}.</li>
 * </ol>
 * The Gradle {@code run}, {@code seed} and {@code searchDemo} tasks set the system
 * property to the project's own {@code owo.db}, so development keeps using the checked-in
 * database. Any other launch method gets a stable per-user location rather than creating
 * an empty database wherever it happened to be started from.
 */
public class DBHelper {
    private static final String DB_PATH_PROPERTY = "owo.db.path";
    private static final String DB_PATH_ENV = "OWO_DB_PATH";

    private static Path databasePath;

    private DBHelper() {
    }

    /** Overrides the database location. Intended for tests; must be called before first use. */
    public static synchronized void setDatabasePath(Path path) {
        databasePath = path;
    }

    public static synchronized Path getDatabasePath() {
        if (databasePath == null) {
            databasePath = resolveDefaultPath();
        }
        return databasePath;
    }

    private static Path resolveDefaultPath() {
        String configured = System.getProperty(DB_PATH_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(DB_PATH_ENV);
        }
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured).toAbsolutePath();
        }
        return Paths.get(System.getProperty("user.home"), ".owo", "owo.db").toAbsolutePath();
    }

    public static Connection getConnection() throws SQLException {
        Path path = getDatabasePath();
        Path parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new SQLException("Cannot create database directory: " + parent, e);
            }
        }
        return DriverManager.getConnection("jdbc:sqlite:" + path);
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
}
