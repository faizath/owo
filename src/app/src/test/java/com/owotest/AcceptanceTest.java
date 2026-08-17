package com.owotest;

import com.owo.controller.AuthController;
import com.owo.controller.CheckInController;
import com.owo.controller.PemesananController;
import com.owo.controller.RefundController;
import com.owo.dao.PemesananDAO;
import com.owo.dao.RefundDAO;
import com.owo.dao.TiketDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.Refund;
import com.owo.entity.TiketPesawat;
import com.owo.utils.DBHelper;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The definition of done for the remediation, end to end against SQLite.
 *
 * <p>Objects are deliberately re-read from the database between steps rather than carried
 * in memory: that is what "quit and relaunch" means for the parts of the sequence that
 * used to pass in memory and revert on restart.
 */
class AcceptanceTest {

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
    void theFullBookingJourneySurvivesARestart() throws Exception {
        // 3. Register a new account.
        AuthController.register("Budi Santoso", "budi@example.com", "password123");

        // 4-5. Relaunch, then sign in with those credentials. Nothing holds the account
        // in memory across this line; it must come back out of SQLite.
        Akun user = AuthController.login("budi@example.com", "password123");
        assertNotNull(user);
        assertEquals("Budi Santoso", user.getNama());

        // 6. Search a seeded route and get real results.
        LocalDateTime departure = LocalDateTime.now().plusDays(9).withNano(0);
        TiketDAO.createTiketPesawat(1_500_000f, true, "GA401", "Jakarta (CGK)",
                "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi", departure);
        TiketDAO.createTiketPesawat(2_500_000f, true, "GA403", "Jakarta (CGK)",
                "Denpasar (DPS)", "Garuda Indonesia", "Bisnis", departure.plusHours(6));

        List<TiketPesawat> results = TiketDAO.searchTiketPesawat(
                "Jakarta", "Denpasar", null, true);
        assertEquals(2, results.size());

        // 7. Select a specific flight, at its own price.
        TiketPesawat chosen = results.stream()
                .filter(f -> f.getFlightNumber().equals("GA403"))
                .findFirst().orElseThrow();
        assertEquals(2_500_000f, chosen.getHarga(), 0.01);

        PemesananController bookings = new PemesananController();
        Pemesanan booking = bookings.createPemesanan(user.getID(), chosen);

        // Availability is claimed, and no catalogue row was invented for the booking.
        assertFalse(TiketDAO.getTiketById(chosen.getId()).isTersedia());
        assertEquals(2, countRows("tiket"));

        // 8. Pay. The booking is confirmed.
        bookings.konfirmasiPemesanan(booking.getId(), user.getID());

        // 9. History shows the booking with the right date *and* time.
        List<Pemesanan> history = PemesananDAO.getPemesananByCustomerId(user.getID());
        assertEquals(1, history.size());
        TiketPesawat booked = (TiketPesawat) history.get(0).getTiket();
        assertEquals(departure.plusHours(6), booked.getWaktuKeberangkatan());
        assertEquals(PemesananStatus.CONFIRMED.dbValue(), history.get(0).getStatus());

        // 12. Request a refund. The tier is the far one, nine days out.
        RefundController refunds = new RefundController(bookings);
        double expected = 2_500_000 * RefundController.TIER_AWAL - RefundController.BIAYA_ADMIN;
        assertEquals(expected,
                refunds.hitungJumlahRefund(PemesananDAO.getPemesananById(booking.getId())), 0.01);

        Refund refund = refunds.ajukanRefund(booking.getId(), user.getID(),
                "Berubah rencana", "Budi Santoso", "BCA 1234567890");

        // 13. Everything is present in the database, re-read rather than remembered.
        Refund stored = RefundDAO.getRefundById(refund.getId());
        assertNotNull(stored);
        assertEquals(expected, stored.getJumlahRefund(), 0.01);
        assertEquals("Budi Santoso", stored.getNamaPenerima());
        assertEquals(PemesananStatus.REFUND_IN_PROGRESS.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());

        assertNoCardDataAnywhere();
    }

    @Test
    void checkInPersistsAcrossARestart() throws Exception {
        AuthController.register("Siti", "siti@example.com", "password123");
        Akun user = AuthController.login("siti@example.com", "password123");

        // Departing later today, so check-in is open now.
        TiketPesawat flight = TiketDAO.createTiketPesawat(900_000f, true, "GA305",
                "Jakarta (CGK)", "Surabaya (SBY)", "Garuda Indonesia", "Ekonomi",
                LocalDateTime.now().withHour(23).withMinute(0).withSecond(0).withNano(0));

        PemesananController bookings = new PemesananController();
        Pemesanan booking = bookings.createPemesanan(user.getID(), flight);
        bookings.konfirmasiPemesanan(booking.getId(), user.getID());

        // 10. Check in.
        new CheckInController(bookings).checkIn(booking.getId(), user.getID());

        // 11. Relaunch: the booking is still checked in. This used to revert, because
        // the status change was applied to the in-memory object only.
        assertEquals(PemesananStatus.CHECKED_IN.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
    }

    /** No table may hold a card number or a CVV, under any column name. */
    private void assertNoCardDataAnywhere() throws Exception {
        List<String> offenders = new ArrayList<>();

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement()) {

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'table'")) {
                while (rs.next()) {
                    tables.add(rs.getString("name"));
                }
            }

            for (String table : tables) {
                try (Statement columnStmt = conn.createStatement();
                     ResultSet rs = columnStmt.executeQuery("PRAGMA table_info(" + table + ")")) {
                    while (rs.next()) {
                        String column = rs.getString("name").toLowerCase();
                        if (column.contains("cvv") || column.contains("nomor_kartu")
                                || column.contains("card_number") || column.contains("expiry")) {
                            offenders.add(table + "." + column);
                        }
                    }
                }
            }
        }

        assertTrue(offenders.isEmpty(), "card columns still present: " + offenders);
    }

    private int countRows(String table) throws Exception {
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
