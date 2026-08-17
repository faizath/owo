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

/**
 * Listing, correcting and deciding refunds.
 *
 * <p>Everything here is about who may see or touch which refund. A refund carries a payee
 * and an account reference, so a listing that is not ownership-scoped, or a lookup whose
 * error message distinguishes "not yours" from "does not exist", hands out other people's
 * disbursement details or the ids needed to go looking for them.
 */
class RefundReviewTest {

    private TempDatabase db;
    private RefundController refundController;
    private Akun customer;
    private Akun stranger;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
        refundController = new RefundController();
        customer = Fixtures.customer();
        stranger = Fixtures.customer();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    /** A refund filed by {@code owner} against a fresh confirmed booking. */
    private Refund fileRefund(Akun owner, String namaPenerima) throws Exception {
        Pemesanan booking = Fixtures.booking(owner, Fixtures.flight(3), PemesananStatus.CONFIRMED);
        return refundController.ajukanRefund(booking.getId(), owner.getID(),
                "Berubah rencana", namaPenerima, "BCA 1234");
    }

    @Test
    void getRefundsForCustomer_returnsOnlyTheirOwnRefunds() throws Exception {
        Refund mine = fileRefund(customer, "Budi Santoso");
        fileRefund(stranger, "Siti Rahayu");

        List<Refund> found = refundController.getRefundsForCustomer(customer.getID());

        // The list is scoped by joining pemesanan. Reading every refund and trusting the
        // page to show the right ones would put another customer's payee on the wire.
        assertEquals(1, found.size());
        assertEquals(mine.getId(), found.get(0).getId());
    }

    @Test
    void getRefundsForCustomer_returnsEveryRefundAcrossTheirBookings() throws Exception {
        fileRefund(customer, "Budi Santoso");
        fileRefund(customer, "Budi Santoso");
        fileRefund(stranger, "Siti Rahayu");

        assertEquals(2, refundController.getRefundsForCustomer(customer.getID()).size());
    }

    @Test
    void getRefundsForCustomer_forSomeoneWithNoRefunds_isEmpty() throws Exception {
        fileRefund(customer, "Budi Santoso");

        assertTrue(refundController.getRefundsForCustomer(stranger.getID()).isEmpty());
    }

    @Test
    void getRefundsForCustomer_carriesTheDisbursementDetails() throws Exception {
        fileRefund(customer, "Budi Santoso");

        Refund listed = refundController.getRefundsForCustomer(customer.getID()).get(0);

        assertEquals("Budi Santoso", listed.getNamaPenerima());
        assertEquals("BCA 1234", listed.getRekeningTujuan());
    }

    @Test
    void getRefundsByStatus_isTheReviewQueue() throws Exception {
        Refund waiting = fileRefund(customer, "Budi Santoso");
        Refund decided = fileRefund(stranger, "Siti Rahayu");
        refundController.setujuiRefund(decided.getId());

        List<Refund> queue = RefundDAO.getRefundsByStatus(Refund.RefundStatus.PENDING_REVIEW);

        assertEquals(1, queue.size());
        assertEquals(waiting.getId(), queue.get(0).getId());
    }

    @Test
    void getRefundsMenungguPeninjauan_spansAllCustomers() throws Exception {
        fileRefund(customer, "Budi Santoso");
        fileRefund(stranger, "Siti Rahayu");

        // The review queue is the one listing that is deliberately not ownership-scoped:
        // an administrator decides everybody's refunds.
        assertEquals(2, refundController.getRefundsMenungguPeninjauan().size());
    }

    @Test
    void getRefundsMenungguPeninjauan_dropsARefundOnceItIsDecided() throws Exception {
        Refund refund = fileRefund(customer, "Budi Santoso");

        refundController.tolakRefund(refund.getId());

        // A rejected refund left in the queue would be decided twice.
        assertTrue(refundController.getRefundsMenungguPeninjauan().isEmpty());
    }

    @Test
    void getOwnedRefund_returnsTheCallersOwnRefund() throws Exception {
        Refund mine = fileRefund(customer, "Budi Santoso");

        Refund loaded = refundController.getOwnedRefund(mine.getId(), customer.getID());

        assertNotNull(loaded);
        assertEquals(mine.getId(), loaded.getId());
    }

    @Test
    void getOwnedRefund_forAnotherCustomersRefund_isRefused() throws Exception {
        Refund theirs = fileRefund(stranger, "Siti Rahayu");

        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.getOwnedRefund(theirs.getId(), customer.getID()));
    }

    @Test
    void getOwnedRefund_cannotBeUsedToDiscoverWhichRefundIdsExist() throws Exception {
        Refund theirs = fileRefund(stranger, "Siti Rahayu");

        PemesananController.PemesananException notYours =
                assertThrows(PemesananController.PemesananException.class,
                        () -> refundController.getOwnedRefund(theirs.getId(), customer.getID()));
        PemesananController.PemesananException noSuchRefund =
                assertThrows(PemesananController.PemesananException.class,
                        () -> refundController.getOwnedRefund("RFD-NOSUCH1", customer.getID()));

        // Distinct wording turns the endpoint into an oracle: walk the id space, and every
        // "not yours" is a confirmed refund belonging to somebody.
        assertEquals(noSuchRefund.getMessage(), notYours.getMessage());
    }

    @Test
    void getOwnedRefund_withANullId_isRefusedRatherThanThrowingNpe() throws Exception {
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.getOwnedRefund(null, customer.getID()));
    }

    @Test
    void perbaruiDetailPencairan_whileAwaitingReview_correctsThePayee() throws Exception {
        Refund refund = fileRefund(customer, "Budi Santoso");

        refundController.perbaruiDetailPencairan(refund.getId(), customer.getID(),
                "Budi Santoso Wijaya", "BCA 9876");

        // The correction has to reach the row: an update that only touched the returned
        // object would pay the original, mistyped account.
        Refund stored = RefundDAO.getRefundById(refund.getId());
        assertEquals("Budi Santoso Wijaya", stored.getNamaPenerima());
        assertEquals("BCA 9876", stored.getRekeningTujuan());
    }

    @Test
    void perbaruiDetailPencairan_onAnotherCustomersRefund_isRefused() throws Exception {
        Refund theirs = fileRefund(stranger, "Siti Rahayu");

        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.perbaruiDetailPencairan(theirs.getId(), customer.getID(),
                        "Budi Santoso", "BCA 9876"));

        Refund stored = RefundDAO.getRefundById(theirs.getId());
        assertEquals("Siti Rahayu", stored.getNamaPenerima(),
                "another customer redirected a refund to their own account");
    }

    @Test
    void perbaruiDetailPencairan_afterApproval_isRefused() throws Exception {
        Refund refund = fileRefund(customer, "Budi Santoso");
        refundController.setujuiRefund(refund.getId());

        // Once the money is authorised the payee is part of the record; changing it after
        // the decision is a redirect nobody reviewed.
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.perbaruiDetailPencairan(refund.getId(), customer.getID(),
                        "Orang Lain", "BCA 0000"));
        assertEquals("Budi Santoso", RefundDAO.getRefundById(refund.getId()).getNamaPenerima());
    }

    @Test
    void perbaruiDetailPencairan_afterRejection_isRefused() throws Exception {
        Refund refund = fileRefund(customer, "Budi Santoso");
        refundController.tolakRefund(refund.getId());

        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.perbaruiDetailPencairan(refund.getId(), customer.getID(),
                        "Orang Lain", "BCA 0000"));
    }

    @Test
    void perbaruiDetailPencairan_withBlankDetails_isRefused() throws Exception {
        Refund refund = fileRefund(customer, "Budi Santoso");

        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.perbaruiDetailPencairan(refund.getId(), customer.getID(),
                        "   ", "BCA 9876"));
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.perbaruiDetailPencairan(refund.getId(), customer.getID(),
                        "Budi Santoso", null));

        // A blank payee is not a correction; it is losing the details the refund needs.
        assertEquals("Budi Santoso", RefundDAO.getRefundById(refund.getId()).getNamaPenerima());
    }

    @Test
    void setujuiRefundById_marksTheRefundApprovedAndTheBookingRefunded() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(3), PemesananStatus.CONFIRMED);
        Refund refund = refundController.ajukanRefund(booking.getId(), customer.getID(),
                "Berubah rencana", "Budi Santoso", "BCA 1234");

        refundController.setujuiRefund(refund.getId());

        assertEquals(Refund.RefundStatus.APPROVED,
                RefundDAO.getRefundById(refund.getId()).getStatus());
        assertEquals(PemesananStatus.REFUNDED.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
    }

    @Test
    void tolakRefundById_restoresThePreviousBookingStatus() throws Exception {
        // Departing today, so it can legitimately be checked in before the refund is filed.
        TiketPesawat flight = Fixtures.flightAt(LocalDateTime.now().plusHours(8), 2_000_000f);
        Pemesanan booking = Fixtures.booking(customer, flight, PemesananStatus.CHECKED_IN);
        Refund refund = refundController.ajukanRefund(booking.getId(), customer.getID(),
                "Berubah rencana", "Budi Santoso", "BCA 1234");

        refundController.tolakRefund(refund.getId());

        // The id-based overload loads the stored row, so it must recover status_sebelumnya
        // from the database rather than from the caller's copy.
        assertEquals(Refund.RefundStatus.REJECTED,
                RefundDAO.getRefundById(refund.getId()).getStatus());
        assertEquals(PemesananStatus.CHECKED_IN.dbValue(),
                PemesananDAO.getPemesananById(booking.getId()).getStatus());
    }

    @Test
    void decidingARefundTwice_isRefused() throws Exception {
        Refund refund = fileRefund(customer, "Budi Santoso");
        refundController.setujuiRefund(refund.getId());

        // The second decision is taken against the stored status, not the in-memory copy
        // an administrator's screen is still holding.
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.tolakRefund(refund.getId()));
        assertEquals(Refund.RefundStatus.APPROVED,
                RefundDAO.getRefundById(refund.getId()).getStatus());
    }

    @Test
    void decidingARefundThatDoesNotExist_isRefused() {
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.setujuiRefund("RFD-NOSUCH1"));
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.tolakRefund("RFD-NOSUCH1"));
        // Cast because the overload set is (String) and (Refund); a null id must be
        // refused with the same message as an unknown one.
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.setujuiRefund((String) null));
        assertThrows(PemesananController.PemesananException.class,
                () -> refundController.tolakRefund((String) null));
    }
}
