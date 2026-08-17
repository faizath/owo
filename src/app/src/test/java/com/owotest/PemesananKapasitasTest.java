package com.owotest;

import com.owo.controller.PemesananController;
import com.owo.dao.AkunDAO;
import com.owo.dao.PemesananDAO;
import com.owo.dao.TiketDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Party size on a booking, and the capacity it is checked against.
 *
 * <p>A party larger than the unit holds is refused twice over: once by the controller, for a
 * message a user can act on, and once by the claiming {@code UPDATE}, which is the check that
 * cannot be bypassed by a caller that goes straight to the DAO.
 */
class PemesananKapasitasTest {

    private TempDatabase db;
    private Akun customer;
    private PemesananController controller;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
        customer = AkunDAO.createAkun("Budi", "budi@example.com", "password123");
        controller = new PemesananController();
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
    void dao_bookingAPartyLargerThanTheTicketHolds_isRefused() throws Exception {
        TiketPesawat seatsTwo = flight("GA401", 2);

        // The controller's check is skippable — this one is the claiming UPDATE's own
        // condition, so nothing that reaches the database can oversell a unit.
        assertThrows(SQLException.class,
                () -> PemesananDAO.createPemesanan(customer.getID(), seatsTwo, 4));
    }

    @Test
    void dao_aRefusedOversizedBooking_leavesTheTicketAvailable() throws Exception {
        TiketPesawat seatsTwo = flight("GA401", 2);

        assertThrows(SQLException.class,
                () -> PemesananDAO.createPemesanan(customer.getID(), seatsTwo, 4));

        // The claim and the insert share one transaction. If the failure escaped after the
        // claim, or without a rollback, the seat would be marked sold with no booking
        // against it and could never be sold again.
        assertTrue(TiketDAO.getTiketById(seatsTwo.getId()).isTersedia(),
                "a refused oversized booking consumed the ticket");
        assertTrue(PemesananDAO.getPemesananByCustomerId(customer.getID()).isEmpty(),
                "a refused oversized booking still wrote a pemesanan row");
    }

    @Test
    void dao_aRefusedOversizedBooking_leavesTheTicketBookableByASmallerParty() throws Exception {
        TiketPesawat seatsTwo = flight("GA401", 2);
        assertThrows(SQLException.class,
                () -> PemesananDAO.createPemesanan(customer.getID(), seatsTwo, 4));

        Pemesanan booked = PemesananDAO.createPemesanan(customer.getID(), seatsTwo, 2);

        assertNotNull(booked);
        assertEquals(2, booked.getJumlahPeserta());
    }

    @Test
    void dao_bookingAPartyOfZero_isRefused() throws Exception {
        TiketPesawat seatsTwo = flight("GA401", 2);

        // Zero would satisfy "kapasitas >= ?" and claim the seat for nobody.
        assertThrows(SQLException.class,
                () -> PemesananDAO.createPemesanan(customer.getID(), seatsTwo, 0));
        assertTrue(TiketDAO.getTiketById(seatsTwo.getId()).isTersedia());
    }

    @Test
    void controller_bookingAPartyLargerThanTheTicketHolds_saysSo() throws Exception {
        TiketPesawat seatsTwo = flight("GA401", 2);

        PemesananController.PemesananException e =
                assertThrows(PemesananController.PemesananException.class,
                        () -> controller.createPemesanan(customer.getID(), seatsTwo, 4));

        // The DAO's message names a row id; the controller exists to say something the
        // person booking can act on.
        assertTrue(e.getMessage().contains("2"), "message did not state the capacity");
        assertTrue(e.getMessage().contains("4"), "message did not state the party size");
    }

    @Test
    void controller_aRefusedOversizedBooking_leavesTheTicketAvailable() throws Exception {
        TiketHotel sleepsTwo = room("101", 2);

        assertThrows(PemesananController.PemesananException.class,
                () -> controller.createPemesanan(customer.getID(), sleepsTwo, 5));

        assertTrue(TiketDAO.getTiketById(sleepsTwo.getId()).isTersedia(),
                "a refused oversized booking consumed the room");
    }

    @Test
    void controller_bookingAPartyThatFits_succeeds() throws Exception {
        TiketPesawat seatsFour = flight("GA401", 4);

        Pemesanan booking = controller.createPemesanan(customer.getID(), seatsFour, 4);

        assertEquals(4, booking.getJumlahPeserta());
        assertTrue(!TiketDAO.getTiketById(seatsFour.getId()).isTersedia(),
                "a successful booking did not claim the ticket");
    }

    @Test
    void jumlahPeserta_roundTripsThroughGetPemesananById() throws Exception {
        Pemesanan created = PemesananDAO.createPemesanan(customer.getID(), flight("GA401", 4), 3);

        Pemesanan reread = PemesananDAO.getPemesananById(created.getId());

        // A party size held only in the returned entity is lost the moment the screen is
        // reloaded, and the boarding list is wrong.
        assertNotNull(reread);
        assertEquals(3, reread.getJumlahPeserta());
    }

    @Test
    void jumlahPeserta_roundTripsThroughGetPemesananByCustomerId() throws Exception {
        PemesananDAO.createPemesanan(customer.getID(), flight("GA401", 4), 3);
        PemesananDAO.createPemesanan(customer.getID(), flight("GA403", 6), 5);

        List<Pemesanan> bookings = PemesananDAO.getPemesananByCustomerId(customer.getID());

        assertEquals(2, bookings.size());
        assertEquals(List.of(3, 5), bookings.stream().map(Pemesanan::getJumlahPeserta).sorted()
                .toList());
    }

    @Test
    void aBookingMadeWithoutAPartySize_countsAsOne() throws Exception {
        Pemesanan created = PemesananDAO.createPemesanan(customer.getID(), flight("GA401", 4));

        assertEquals(1, created.getJumlahPeserta());
        assertEquals(1, PemesananDAO.getPemesananById(created.getId()).getJumlahPeserta());
    }

    @Test
    void controller_bookingAnAlreadyClaimedTicket_isRefused() throws Exception {
        TiketPesawat seatsFour = flight("GA401", 4);
        controller.createPemesanan(customer.getID(), seatsFour, 2);

        // The unit is sold whole: the remaining two seats are not separately bookable.
        Akun other = AkunDAO.createAkun("Siti", "siti@example.com", "password123");
        assertThrows(PemesananController.PemesananException.class,
                () -> controller.createPemesanan(other.getID(),
                        TiketDAO.getTiketById(seatsFour.getId()), 2));
    }
}
