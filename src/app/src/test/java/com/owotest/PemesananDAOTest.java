package com.owotest;

import com.owo.dao.AkunDAO;
import com.owo.dao.PemesananDAO;
import com.owo.dao.TiketDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.TiketPesawat;
import com.owo.utils.DBHelper;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PemesananDAOTest {

    private TempDatabase db;
    private Akun customer;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
        customer = AkunDAO.createAkun("Budi", "budi@example.com", "password123");
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private TiketPesawat flight(String flightNumber, int daysAhead) throws Exception {
        return TiketDAO.createTiketPesawat(1_500_000f, true, flightNumber,
                "Jakarta (CGK)", "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi",
                LocalDateTime.now().plusDays(daysAhead).withNano(0));
    }

    @Test
    void getPemesananByCustomerId_withTwoBookings_returnsBoth() throws Exception {
        PemesananDAO.createPemesanan(customer.getID(), flight("GA401", 3));
        PemesananDAO.createPemesanan(customer.getID(), flight("GA403", 4));

        // With a shared static connection, the nested TiketDAO lookup closed the
        // ResultSet being iterated here. One booking passed; two threw.
        List<Pemesanan> bookings = PemesananDAO.getPemesananByCustomerId(customer.getID());

        assertEquals(2, bookings.size());
        assertTrue(bookings.stream().allMatch(p -> p.getTiket() != null));
    }

    @Test
    void getPemesananByCustomerId_withFiveBookings_returnsAll() throws Exception {
        for (int i = 0; i < 5; i++) {
            PemesananDAO.createPemesanan(customer.getID(), flight("GA50" + i, i + 1));
        }

        List<Pemesanan> bookings = PemesananDAO.getPemesananByCustomerId(customer.getID());

        assertEquals(5, bookings.size());
    }

    @Test
    void getPemesananById_returnsBookingIdNotTicketId() throws Exception {
        // Force the two id sequences apart, so a mix-up is detectable.
        flight("PAD1", 1);
        flight("PAD2", 2);
        flight("PAD3", 3);
        TiketPesawat booked = flight("GA401", 4);

        Pemesanan created = PemesananDAO.createPemesanan(customer.getID(), booked);
        assertNotEquals(created.getId(), booked.getId(),
                "test setup failed to make the ids diverge");

        Pemesanan fetched = PemesananDAO.getPemesananById(created.getId());

        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals(booked.getId(), fetched.getTiket().getId());
    }

    @Test
    void createPemesanan_thenUpdateStatus_persists() throws Exception {
        Pemesanan created = PemesananDAO.createPemesanan(customer.getID(), flight("GA401", 3));

        PemesananDAO.updateStatus(created.getId(), PemesananStatus.CONFIRMED);

        Pemesanan reread = PemesananDAO.getPemesananById(created.getId());
        assertNotNull(reread);
        assertEquals(PemesananStatus.CONFIRMED.dbValue(), reread.getStatus());
    }

    @Test
    void getPemesanan_withSqliteNativeTimestamp_doesNotThrow() throws Exception {
        TiketPesawat tiket = flight("GA401", 3);

        // SQLite's own CURRENT_TIMESTAMP uses a space separator, not 'T'.
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "INSERT INTO pemesanan (customer_id, tiket_id, tanggal_pesan, status) VALUES ("
                            + customer.getID() + ", " + tiket.getId()
                            + ", CURRENT_TIMESTAMP, 'PENDING')");
        }

        List<Pemesanan> bookings = assertDoesNotThrow(
                () -> PemesananDAO.getPemesananByCustomerId(customer.getID()));
        assertEquals(1, bookings.size());
        assertNotNull(bookings.get(0).getTanggalPesan());
    }
}
