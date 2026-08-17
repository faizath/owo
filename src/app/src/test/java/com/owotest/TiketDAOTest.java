package com.owotest;

import com.owo.dao.TiketDAO;
import com.owo.entity.Tiket;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;
import com.owo.utils.DBHelper;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TiketDAOTest {

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
    void searchHotels_stayInsideListingWindow_returnsListing() throws Exception {
        LocalDate listingIn = LocalDate.now().plusDays(3);
        LocalDate listingOut = LocalDate.now().plusDays(13);
        TiketDAO.createTiketHotel(750_000f, true, listingIn, listingOut,
                "Nandini Jungle Resort & Spa", "101", "Ubud, Bali");

        // A stay of days 5-8 sits inside the listing's 3-13 window.
        List<TiketHotel> found = TiketDAO.searchTiketHotel(null,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(8), null, true);

        assertEquals(1, found.size(), "a stay inside the listing window was not matched");
    }

    @Test
    void searchHotels_stayOutsideListingWindow_returnsNothing() throws Exception {
        TiketDAO.createTiketHotel(750_000f, true,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(6),
                "Nandini Jungle Resort & Spa", "101", "Ubud, Bali");

        // A stay of days 20-25 is entirely outside the listing's 3-6 window.
        List<TiketHotel> found = TiketDAO.searchTiketHotel(null,
                LocalDate.now().plusDays(20), LocalDate.now().plusDays(25), null, true);

        assertTrue(found.isEmpty(), "a stay outside the listing window was matched");
    }

    @Test
    void getTiketById_unknownTipe_failsLoudly() throws Exception {
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO tiket (harga, tersedia, tipe) VALUES (100, 1, 'KAPAL')");
        }

        // Returning null here used to surface much later as an NPE on Pemesanan.getTiket().
        assertThrows(SQLException.class, () -> TiketDAO.getTiketById(1));
    }

    @Test
    void getTiketById_returnsFlightFieldsForFlight() throws Exception {
        LocalDateTime departure = LocalDateTime.now().plusDays(3).withNano(0);
        TiketPesawat created = TiketDAO.createTiketPesawat(1_500_000f, true, "GA401",
                "Jakarta (CGK)", "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi", departure);

        Tiket fetched = TiketDAO.getTiketById(created.getId());

        TiketPesawat flight = assertInstanceOf(TiketPesawat.class, fetched);
        assertEquals("GA401", flight.getFlightNumber());
        assertEquals("Garuda Indonesia", flight.getMaskapai());
        assertEquals("Jakarta (CGK)", flight.getOrigin());
        assertEquals("Denpasar (DPS)", flight.getDestination());
        assertEquals("Ekonomi", flight.getKelas());
        assertEquals(departure, flight.getWaktuKeberangkatan());
    }

    @Test
    void getTiketById_returnsHotelFieldsForHotel() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(8);
        TiketHotel created = TiketDAO.createTiketHotel(750_000f, true, checkIn, checkOut,
                "Nandini Jungle Resort & Spa", "101", "Ubud, Bali");

        Tiket fetched = TiketDAO.getTiketById(created.getId());

        TiketHotel hotel = assertInstanceOf(TiketHotel.class, fetched);
        assertEquals("Nandini Jungle Resort & Spa", hotel.getHotelName());
        assertEquals("101", hotel.getRoomNumber());
        assertEquals(checkIn, hotel.getCheckIn());
        assertEquals(checkOut, hotel.getCheckOut());
    }

    @Test
    void createTiketPesawat_leavesNoOrphanWhenSubtypeInsertFails() throws Exception {
        // A null flight number violates NOT NULL on tiket_pesawat, after the base
        // tiket row has already been inserted inside the same transaction.
        assertThrows(SQLException.class, () -> TiketDAO.createTiketPesawat(
                1_500_000f, true, null, "Jakarta", "Denpasar", "Garuda", "Ekonomi",
                LocalDateTime.now().plusDays(3)));

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM tiket")) {
            rs.next();
            assertEquals(0, rs.getInt(1), "rollback left an orphan tiket row");
        }
    }

    @Test
    void searchFlights_filtersByClassUsingIndonesianVocabulary() throws Exception {
        LocalDateTime departure = LocalDateTime.now().plusDays(3).withNano(0);
        TiketDAO.createTiketPesawat(1_500_000f, true, "GA401", "Jakarta", "Denpasar",
                "Garuda Indonesia", "Ekonomi", departure);
        TiketDAO.createTiketPesawat(2_500_000f, true, "GA403", "Jakarta", "Denpasar",
                "Garuda Indonesia", "Bisnis", departure);

        List<TiketPesawat> bisnis = TiketDAO.searchTiketPesawat(null, null, "Bisnis", true);

        assertEquals(1, bisnis.size());
        assertEquals("GA403", bisnis.get(0).getFlightNumber());
    }
}
