package com.owotest;

import com.owo.controller.CheckInController;
import com.owo.controller.PemesananController;
import com.owo.dao.PemesananDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Check-in against the database. {@link CheckInTest} covers the in-memory rules; this
 * covers the parts that only fail once persistence is involved.
 */
class CheckInPersistenceTest {

    private TempDatabase db;
    private CheckInController checkInController;
    private Akun customer;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
        checkInController = new CheckInController();
        customer = Fixtures.customer();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void checkIn_onTheDepartureDate_persists() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(0),
                PemesananStatus.CONFIRMED);

        checkInController.checkIn(booking.getId(), customer.getID());

        // The status change used to be applied in memory only and reverted on restart.
        assertEquals(PemesananStatus.CHECKED_IN.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
    }

    @Test
    void checkIn_afterTheDepartureDate_isRefused() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(-3),
                PemesananStatus.CONFIRMED);

        // Only the "too early" half of the rule was enforced, so check-in succeeded
        // months after departure — and the old suite had no case for it.
        assertThrows(PemesananController.PemesananException.class,
                () -> checkInController.checkIn(booking.getId(), customer.getID()));

        assertEquals(PemesananStatus.CONFIRMED.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
    }

    @Test
    void checkIn_beforeTheDepartureDate_isRefused() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(5),
                PemesananStatus.CONFIRMED);

        assertThrows(PemesananController.PemesananException.class,
                () -> checkInController.checkIn(booking.getId(), customer.getID()));
    }

    @Test
    void checkIn_onAnUnpaidBooking_isRefused() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(0),
                PemesananStatus.PENDING);

        assertThrows(PemesananController.PemesananException.class,
                () -> checkInController.checkIn(booking.getId(), customer.getID()));
    }

    @Test
    void checkIn_onACancelledBooking_isRefused() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(0),
                PemesananStatus.CANCELLED);

        assertThrows(PemesananController.PemesananException.class,
                () -> checkInController.checkIn(booking.getId(), customer.getID()));
    }

    @Test
    void checkIn_onAnotherUsersBooking_isRefused() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(0),
                PemesananStatus.CONFIRMED);
        Akun stranger = Fixtures.customer();

        // Any user could previously check into any booking id.
        assertThrows(PemesananController.PemesananException.class,
                () -> checkInController.checkIn(booking.getId(), stranger.getID()));
    }

    @Test
    void checkIn_onANonexistentBooking_isRefused() {
        // updateStatus on a missing id affected zero rows and still returned success.
        assertThrows(PemesananController.PemesananException.class,
                () -> checkInController.checkIn(9999, customer.getID()));
    }

    @Test
    void checkIn_onAHotelOnItsCheckInDate_persists() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.hotel(0),
                PemesananStatus.CONFIRMED);

        checkInController.checkIn(booking.getId(), customer.getID());

        assertEquals(PemesananStatus.CHECKED_IN.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
    }
}
