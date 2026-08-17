package com.owotest;

import com.owo.utils.DBHelper;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DBHelperTest {

    private TempDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void getConnection_returnsDistinctUsableConnections() throws Exception {
        try (Connection first = DBHelper.getConnection()) {
            Connection second = DBHelper.getConnection();
            second.close();

            // Closing one connection must not close the other. When a single static
            // connection was shared, this left `first` unusable.
            assertFalse(first.isClosed(), "closing one connection closed the other");
            try (Statement stmt = first.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
            }
        }
    }

    @Test
    void initializeDatabase_isIdempotent() {
        assertDoesNotThrow(DBHelper::initializeDatabase);
        assertDoesNotThrow(DBHelper::initializeDatabase);
    }

    @Test
    void initializeDatabase_createsAllSevenTables() throws Exception {
        Set<String> expected = Set.of(
                "akun", "tiket", "tiket_pesawat", "tiket_hotel",
                "pemesanan", "refund", "notifikasi");

        Set<String> actual = new HashSet<>();
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'table'")) {
            while (rs.next()) {
                actual.add(rs.getString("name"));
            }
        }

        assertTrue(actual.containsAll(expected),
                "missing tables: " + expected.stream().filter(t -> !actual.contains(t)).toList());
    }
}
