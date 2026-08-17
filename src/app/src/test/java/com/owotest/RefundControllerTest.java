package com.owotest;

import com.owo.controller.PemesananController;
import com.owo.controller.RefundController;
import com.owo.dao.PemesananDAO;
import com.owo.dao.RefundDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.Refund;
import com.owo.entity.TiketPesawat;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefundControllerTest {

    private TempDatabase db;
    private RefundController refundController;
    private Akun customer;
    private Akun reviewer;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
        refundController = new RefundController();
        customer = Fixtures.customer();
        reviewer = Fixtures.admin();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private Pemesanan confirmedBooking(TiketPesawat flight) throws Exception {
        return Fixtures.booking(customer, flight, PemesananStatus.CONFIRMED);
    }

    @Test
    void aDepartureJustOverSevenDaysAwayGetsTheHigherTier() throws Exception {
        // 7 days and 23 hours. Truncating day arithmetic scored this as 7 and charged
        // the lower tier.
        TiketPesawat flight = Fixtures.flightAt(
                LocalDateTime.now().plusDays(7).plusHours(23), 1_000_000f);

        double amount = refundController.hitungJumlahRefund(confirmedBooking(flight));

        assertEquals(1_000_000 * RefundController.TIER_AWAL - RefundController.BIAYA_ADMIN,
                amount, 0.01);
    }

    @Test
    void aDepartureInsideSevenDaysGetsTheLowerTier() throws Exception {
        TiketPesawat flight = Fixtures.flightAt(
                LocalDateTime.now().plusDays(3), 1_000_000f);

        double amount = refundController.hitungJumlahRefund(confirmedBooking(flight));

        assertEquals(1_000_000 * RefundController.TIER_AKHIR - RefundController.BIAYA_ADMIN,
                amount, 0.01);
    }

    @Test
    void anAlreadyDepartedTicketIsNotRefundable() throws Exception {
        TiketPesawat flight = Fixtures.flightAt(LocalDateTime.now().minusDays(2), 1_000_000f);
        Pemesanan booking = confirmedBooking(flight);

        // A negative day difference used to land in the 75% tier and refund happily.
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.hitungJumlahRefund(booking));
    }

    @Test
    void aRefundWorthLessThanTheAdminFeeIsRefused() throws Exception {
        // 75% of Rp 60,000 is Rp 45,000, below the Rp 50,000 fee.
        TiketPesawat flight = Fixtures.flightAt(LocalDateTime.now().plusDays(3), 60_000f);
        Pemesanan booking = confirmedBooking(flight);

        // This used to clamp to zero and report "Refund berhasil diajukan... IDR 0.00".
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.hitungJumlahRefund(booking));
    }

    @Test
    void ajukanRefund_persistsAndMovesTheBooking() throws Exception {
        Pemesanan booking = confirmedBooking(Fixtures.flight(3));

        Refund refund = refundController.ajukanRefund(booking.getId(), customer.getID(),
                "Berubah rencana", "Budi Santoso", "BCA 1234");

        assertNotNull(RefundDAO.getRefundById(refund.getId()));
        assertEquals(PemesananStatus.REFUND_IN_PROGRESS.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
    }

    @Test
    void ajukanRefund_persistsTheDisbursementDetails() throws Exception {
        Pemesanan booking = confirmedBooking(Fixtures.flight(3));

        Refund refund = refundController.ajukanRefund(booking.getId(), customer.getID(),
                "Berubah rencana", "Budi Santoso", "BCA 1234");

        // updateDetailKartu had no callers, so these were silently discarded and the
        // refund could never be paid out.
        Refund stored = RefundDAO.getRefundById(refund.getId());
        assertEquals("Budi Santoso", stored.getNamaPenerima());
        assertEquals("BCA 1234", stored.getRekeningTujuan());
    }

    @Test
    void ajukanRefund_generatesItsOwnId() throws Exception {
        Pemesanan first = confirmedBooking(Fixtures.flight(3));
        Pemesanan second = confirmedBooking(Fixtures.flight(4));

        Refund a = refundController.ajukanRefund(first.getId(), customer.getID(),
                "Alasan", "Budi", "BCA 1");
        Refund b = refundController.ajukanRefund(second.getId(), customer.getID(),
                "Alasan", "Budi", "BCA 2");

        assertTrue(a.getId().startsWith("RFD-"));
        assertTrue(!a.getId().equals(b.getId()));
    }

    @Test
    void ajukanRefund_withoutDisbursementDetails_reportsAUsefulMessage() throws Exception {
        Pemesanan booking = confirmedBooking(Fixtures.flight(3));

        // This used to dereference a null map and surface the message "null".
        PemesananController.PemesananException e =
                assertThrows(PemesananController.PemesananException.class,
                        () -> refundController.ajukanRefund(booking.getId(), customer.getID(),
                                "Alasan", null, null));
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().toLowerCase().contains("penerima"));
    }

    @Test
    void ajukanRefund_onAnotherUsersBooking_isRefused() throws Exception {
        Pemesanan booking = confirmedBooking(Fixtures.flight(3));
        Akun stranger = Fixtures.customer();

        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.ajukanRefund(booking.getId(), stranger.getID(),
                        "Alasan", "Budi", "BCA 1"));
    }

    @Test
    void tolakRefund_restoresACheckedInBookingRatherThanDowngradingIt() throws Exception {
        // Departing today, so it can legitimately be checked in.
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flightAt(
                LocalDateTime.now().plusHours(8), 2_000_000f), PemesananStatus.CHECKED_IN);

        Refund refund = refundController.ajukanRefund(booking.getId(), customer.getID(),
                "Alasan", "Budi", "BCA 1");
        refundController.tolakRefund(refund.getId(), reviewer.getID());

        // Rejection used to write CONFIRMED unconditionally, re-enabling check-in.
        assertEquals(PemesananStatus.CHECKED_IN.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
    }

    @Test
    void setujuiRefund_marksTheBookingRefundedAndReleasesTheTicket() throws Exception {
        TiketPesawat flight = Fixtures.flight(3);
        Pemesanan booking = confirmedBooking(flight);

        Refund refund = refundController.ajukanRefund(booking.getId(), customer.getID(),
                "Alasan", "Budi", "BCA 1");
        refundController.setujuiRefund(refund.getId(), reviewer.getID());

        assertEquals(PemesananStatus.REFUNDED.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
        assertTrue(com.owo.dao.TiketDAO.getTiketById(flight.getId()).isTersedia(),
                "an approved refund did not return the ticket to the catalogue");
    }

    @Test
    void refundsAreListedForTheirBooking() throws Exception {
        Pemesanan booking = confirmedBooking(Fixtures.flight(3));
        refundController.ajukanRefund(booking.getId(), customer.getID(),
                "Alasan", "Budi", "BCA 1");

        List<Refund> refunds = refundController.getRefundByPemesananId(booking.getId());

        assertEquals(1, refunds.size());
    }
}
