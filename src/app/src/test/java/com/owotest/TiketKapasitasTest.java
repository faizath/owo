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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Capacity on {@code tiket}, and the search filter built on it.
 *
 * <p>The party size used to be accepted by the search methods and dropped into an empty
 * {@code if} block, so a family of four was shown single rooms and single seats and only
 * discovered the mismatch at the counter.
 */
class TiketKapasitasTest {

    private TempDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private TiketPesawat flight(String flightNumber, int kapasitas) throws Exception {
        return TiketDAO.createTiketPesawat(1_500_000f, true, flightNumber,
                "Jakarta (CGK)", "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi",
                LocalDateTime.now().plusDays(3).withNano(0), kapasitas);
    }

    private TiketHotel room(String roomNumber, int kapasitas) throws Exception {
        return TiketDAO.createTiketHotel(750_000f, true,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(13),
                "Nandini Jungle Resort & Spa", roomNumber, "Ubud, Bali", kapasitas);
    }

    @Test
    void createTiketPesawat_persistsTheStatedCapacity() throws Exception {
        TiketPesawat created = flight("GA401", 4);

        Tiket reread = TiketDAO.getTiketById(created.getId());

        // The returned entity and the row must agree; a capacity that only exists in Java
        // is a filter the next search will not apply.
        assertEquals(4, created.getKapasitas());
        assertEquals(4, reread.getKapasitas());
    }

    @Test
    void createTiketHotel_persistsTheStatedCapacity() throws Exception {
        TiketHotel created = room("101", 3);

        assertEquals(3, TiketDAO.getTiketById(created.getId()).getKapasitas());
    }

    @Test
    void createTiket_withoutACapacity_holdsOnePerson() throws Exception {
        TiketPesawat seat = TiketDAO.createTiketPesawat(1_500_000f, true, "GA401",
                "Jakarta (CGK)", "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi",
                LocalDateTime.now().plusDays(3).withNano(0));
        TiketHotel single = TiketDAO.createTiketHotel(750_000f, true,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(13),
                "Nandini Jungle Resort & Spa", "101", "Ubud, Bali");

        assertEquals(1, TiketDAO.getTiketById(seat.getId()).getKapasitas());
        assertEquals(1, TiketDAO.getTiketById(single.getId()).getKapasitas());
    }

    @Test
    void createTiket_withACapacityBelowOne_isRefusedAndLeavesNoRow() throws Exception {
        assertThrows(SQLException.class, () -> flight("GA401", 0));

        // The base row is inserted first, so a capacity rejected after the fact would leave
        // an orphan tiket with no tiket_pesawat behind it — invisible to search, but still
        // there.
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tiket")) {
            rs.next();
            assertEquals(0, rs.getInt(1), "a rejected capacity left a tiket row behind");
        }
    }

    @Test
    void searchFlights_forAPartyLargerThanEveryListing_findsNothing() throws Exception {
        flight("GA401", 1);
        flight("GA403", 2);

        List<TiketPesawat> found = TiketDAO.searchTiketPesawat(null, null, null, true, 4);

        // Returning these anyway is the original defect: the party size was accepted and
        // then ignored.
        assertTrue(found.isEmpty(), "a party of four was shown seats that hold at most two");
    }

    @Test
    void searchFlights_forAPartyThatFits_findsTheListing() throws Exception {
        flight("GA401", 2);
        flight("GA403", 5);

        List<TiketPesawat> found = TiketDAO.searchTiketPesawat(null, null, null, true, 4);

        assertEquals(1, found.size());
        assertEquals("GA403", found.get(0).getFlightNumber());
    }

    @Test
    void searchFlights_forAPartyExactlyFillingTheListing_findsIt() throws Exception {
        flight("GA401", 4);

        // The filter is >=, not >; a boundary written as strictly-greater would hide the
        // only listing that fits the party exactly.
        assertEquals(1, TiketDAO.searchTiketPesawat(null, null, null, true, 4).size());
    }

    @Test
    void searchHotels_forAPartyLargerThanEveryRoom_findsNothing() throws Exception {
        room("101", 2);
        room("102", 1);

        List<TiketHotel> found = TiketDAO.searchTiketHotel(null, null, null, null, true, 4);

        assertTrue(found.isEmpty(), "a party of four was shown rooms that sleep at most two");
    }

    @Test
    void searchHotels_forAPartyThatFits_findsTheRoom() throws Exception {
        room("101", 2);
        room("102", 6);

        List<TiketHotel> found = TiketDAO.searchTiketHotel(null, null, null, null, true, 4);

        assertEquals(1, found.size());
        assertEquals("102", found.get(0).getRoomNumber());
    }

    @Test
    void search_withoutAPartySize_stillReturnsSingleOccupancyListings() throws Exception {
        flight("GA401", 1);
        room("101", 1);

        // The capacity clause is only added when a party larger than one is asked for, so
        // every existing caller keeps its old result set.
        assertEquals(1, TiketDAO.searchTiketPesawat(null, null, null, true).size());
        assertEquals(1, TiketDAO.searchTiketHotel(null, null, null, null, true).size());
        assertEquals(1, TiketDAO.searchTiketPesawat(null, null, null, true, 1).size());
        assertEquals(1, TiketDAO.searchTiketHotel(null, null, null, null, true, 1).size());
    }

    @Test
    void search_appliesTheCapacityFilterAlongsideTheOtherFilters() throws Exception {
        flight("GA401", 6);
        TiketDAO.createTiketPesawat(2_500_000f, true, "GA403", "Jakarta (CGK)",
                "Surabaya (SUB)", "Garuda Indonesia", "Ekonomi",
                LocalDateTime.now().plusDays(3).withNano(0), 6);

        List<TiketPesawat> found =
                TiketDAO.searchTiketPesawat(null, "Denpasar", null, true, 4);

        // The capacity parameter is appended after the others; a mis-numbered placeholder
        // would bind the party size to the destination.
        assertEquals(1, found.size());
        assertEquals("GA401", found.get(0).getFlightNumber());
    }

    @Test
    void search_forAPartyThatFits_ignoresAlreadyClaimedListings() throws Exception {
        TiketPesawat big = flight("GA401", 6);
        TiketDAO.createTiketPesawat(1_500_000f, false, "GA403", "Jakarta (CGK)",
                "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi",
                LocalDateTime.now().plusDays(3).withNano(0), 6);

        List<TiketPesawat> found = TiketDAO.searchTiketPesawat(null, null, null, true, 4);

        assertEquals(1, found.size());
        assertEquals(big.getId(), found.get(0).getId());
    }

    @Test
    void search_offersTheSmallestUnitThatFitsFirst() throws Exception {
        // The same departure for both, so capacity is what separates them in the ordering.
        LocalDateTime departure = LocalDate.now().plusDays(5).atTime(9, 0);
        TiketPesawat family = TiketDAO.createTiketPesawat(1_000_000f, true, "GA501",
                "Jakarta (CGK)", "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi",
                departure, 4);
        TiketPesawat single = TiketDAO.createTiketPesawat(1_200_000f, true, "GA502",
                "Jakarta (CGK)", "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi",
                departure, 1);

        List<TiketPesawat> found = TiketDAO.searchTiketPesawat(null, null, null, true, 1);

        // One booking claims a whole unit, so putting the four-seat block in front of a
        // solo traveller — as sorting by price alone did — denies it to a party of four.
        assertEquals(single.getId(), found.get(0).getId());
        assertEquals(family.getId(), found.get(1).getId());
    }

    @Test
    void search_forAPartyOfOne_stillExcludesNothingItCanUse() throws Exception {
        flight("GA401", 6);

        // The filter used to be skipped entirely for a party of one. Applying it changes
        // no result, because a unit always holds at least one person — which is exactly
        // why the special case was worth removing rather than documenting.
        assertEquals(1, TiketDAO.searchTiketPesawat(null, null, null, true, 1).size());
    }
}
