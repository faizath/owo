package com.owotest;

import com.owo.controller.PemesananController;
import com.owo.dao.PemesananDAO;
import com.owo.dao.TiketDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.TiketPesawat;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PemesananControllerTest {

    private TempDatabase db;
    private PemesananController controller;
    private Akun customer;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
        controller = new PemesananController();
        customer = Fixtures.customer();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void createPemesanan_claimsTheTicket() throws Exception {
        TiketPesawat flight = Fixtures.flight(3);

        controller.createPemesanan(customer.getID(), flight);

        // Availability was never decremented, so the same seat could be sold repeatedly.
        assertFalse(TiketDAO.getTiketById(flight.getId()).isTersedia());
    }

    @Test
    void createPemesanan_doesNotAddACatalogueRow() throws Exception {
        TiketPesawat flight = Fixtures.flight(3);
        int before = TiketDAO.getAllAvailableFlights().size();

        controller.createPemesanan(customer.getID(), flight);

        // Booking used to insert a brand new ticket row, multiplying search results.
        assertEquals(before - 1, TiketDAO.getAllAvailableFlights().size());
    }

    @Test
    void createPemesanan_onAClaimedTicket_isRefused() throws Exception {
        TiketPesawat flight = Fixtures.flight(3);
        controller.createPemesanan(customer.getID(), flight);

        Akun other = Fixtures.customer();
        assertThrows(PemesananController.PemesananException.class,
                () -> controller.createPemesanan(other.getID(), flight));
    }

    @Test
    void konfirmasiPemesanan_movesPendingToConfirmed() throws Exception {
        Pemesanan booking = controller.createPemesanan(customer.getID(), Fixtures.flight(3));

        controller.konfirmasiPemesanan(booking.getId(), customer.getID());

        assertEquals(PemesananStatus.CONFIRMED.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
    }

    @Test
    void getOwnedPemesanan_refusesAnotherUsersBooking() throws Exception {
        Pemesanan booking = controller.createPemesanan(customer.getID(), Fixtures.flight(3));
        Akun stranger = Fixtures.customer();

        assertThrows(PemesananController.PemesananException.class,
                () -> controller.getOwnedPemesanan(booking.getId(), stranger.getID()));
    }

    @Test
    void getOwnedPemesanan_refusesANonexistentBooking() {
        assertThrows(PemesananController.PemesananException.class,
                () -> controller.getOwnedPemesanan(9999, customer.getID()));
    }

    @Test
    void aNonexistentBookingReportsTheSameMessageAsSomeoneElses() throws Exception {
        Pemesanan booking = controller.createPemesanan(customer.getID(), Fixtures.flight(3));
        Akun stranger = Fixtures.customer();

        String foreign = assertThrows(PemesananController.PemesananException.class,
                () -> controller.getOwnedPemesanan(booking.getId(), stranger.getID())).getMessage();
        String missing = assertThrows(PemesananController.PemesananException.class,
                () -> controller.getOwnedPemesanan(9999, stranger.getID())).getMessage();

        // Different messages would let a client probe for other users' booking ids.
        assertEquals(foreign, missing);
    }

    @Test
    void batalkanPemesanan_releasesTheTicket() throws Exception {
        TiketPesawat flight = Fixtures.flight(3);
        Pemesanan booking = controller.createPemesanan(customer.getID(), flight);

        controller.batalkanPemesanan(booking.getId(), customer.getID());

        assertEquals(PemesananStatus.CANCELLED.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
        assertTrue(TiketDAO.getTiketById(flight.getId()).isTersedia());
    }

    @Test
    void aRefundedBookingCannotBeCancelled() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(3), PemesananStatus.REFUNDED);

        // Cancelling a refunded booking used to succeed, orphaning its refund record.
        assertThrows(PemesananController.PemesananException.class,
                () -> controller.batalkanPemesanan(booking.getId(), customer.getID()));
    }

    @Test
    void aCheckedInBookingCannotBeConfirmedAgain() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(3),
                PemesananStatus.CHECKED_IN);

        assertThrows(PemesananController.PemesananException.class,
                () -> controller.konfirmasiPemesanan(booking.getId(), customer.getID()));
    }

    @Test
    void updateStatus_onANonexistentBooking_reportsFailure() {
        // An update affecting zero rows used to be reported to the caller as success.
        assertThrows(java.sql.SQLException.class,
                () -> PemesananDAO.updateStatus(9999, PemesananStatus.CHECKED_IN));
    }
}
