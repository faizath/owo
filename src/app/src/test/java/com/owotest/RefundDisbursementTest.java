package com.owotest;

import com.owo.controller.PemesananController;
import com.owo.controller.RefundController;
import com.owo.dao.RefundDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.Refund;
import com.owo.entity.Refund.RefundStatus;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paying an approved refund out.
 *
 * <p>PROCESSING, COMPLETED and FAILED were enum constants with no writer anywhere in
 * {@code main}: an approved refund stayed under "Sedang Diproses" indefinitely and the
 * "Selesai" tab could only ever contain rejections. Approval authorises the payment;
 * these steps record it actually being made.
 */
class RefundDisbursementTest {

    private TempDatabase db;
    private PemesananController pemesanan;
    private RefundController refund;
    private Akun customer;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
        pemesanan = new PemesananController();
        refund = new RefundController(pemesanan);
        customer = Fixtures.customer();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    /** A refund that has been filed and approved, ready to be paid. */
    private Refund approved() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(30),
                PemesananStatus.CONFIRMED);
        Refund filed = refund.ajukanRefund(booking.getId(), customer.getID(),
                "Berhalangan hadir", "Faiz A", "1234567890");
        return refund.setujuiRefund(filed.getId());
    }

    private RefundStatus stored(String refundId) throws Exception {
        return RefundDAO.getRefundById(refundId).getStatus();
    }

    @Test
    void anApprovedRefundCanBePaidOutToCompletion() throws Exception {
        Refund filed = approved();

        refund.prosesRefund(filed.getId());
        assertEquals(RefundStatus.PROCESSING, stored(filed.getId()));

        refund.selesaikanRefund(filed.getId());
        assertEquals(RefundStatus.COMPLETED, stored(filed.getId()));
    }

    @Test
    void aFailedDisbursementCanBeRetried() throws Exception {
        Refund filed = approved();
        refund.prosesRefund(filed.getId());

        refund.gagalkanRefund(filed.getId());
        assertEquals(RefundStatus.FAILED, stored(filed.getId()));

        // The approval still stands, so a retry must not go back through review.
        refund.prosesRefund(filed.getId());
        assertEquals(RefundStatus.PROCESSING, stored(filed.getId()));
    }

    @Test
    void aRefundStillUnderReviewCannotBePaid() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(30),
                PemesananStatus.CONFIRMED);
        Refund filed = refund.ajukanRefund(booking.getId(), customer.getID(),
                "Berhalangan hadir", "Faiz A", "1234567890");

        assertThrows(PemesananController.PemesananException.class,
                () -> refund.prosesRefund(filed.getId()));
        assertEquals(RefundStatus.PENDING_REVIEW, stored(filed.getId()));
    }

    @Test
    void aCompletedRefundIsTerminal() throws Exception {
        Refund filed = approved();
        refund.prosesRefund(filed.getId());
        refund.selesaikanRefund(filed.getId());

        assertTrue(RefundStatus.COMPLETED.isTerminal());
        assertThrows(PemesananController.PemesananException.class,
                () -> refund.prosesRefund(filed.getId()));
    }

    @Test
    void advanceStatus_refusesARefundThatMovedOn() throws Exception {
        Refund filed = approved();
        refund.prosesRefund(filed.getId());

        // Two administrators working the same queue: the second read APPROVED, but the
        // first has already started the payment. Writing anyway would undo that.
        assertFalse(RefundDAO.advanceStatus(
                filed.getId(), RefundStatus.APPROVED, RefundStatus.PROCESSING));
    }

    @Test
    void theWorkQueueKeepsARefundVisibleUntilItIsPaid() throws Exception {
        Refund filed = approved();

        // Scoping the queue to PENDING_REVIEW is exactly why an approved refund had
        // nowhere left to go.
        assertTrue(ids(refund.getRefundsAktif()).contains(filed.getId()));

        refund.prosesRefund(filed.getId());
        assertTrue(ids(refund.getRefundsAktif()).contains(filed.getId()));

        refund.selesaikanRefund(filed.getId());
        assertFalse(ids(refund.getRefundsAktif()).contains(filed.getId()));
    }

    @Test
    void aRejectedRefundLeavesTheWorkQueue() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(30),
                PemesananStatus.CONFIRMED);
        Refund filed = refund.ajukanRefund(booking.getId(), customer.getID(),
                "Berhalangan hadir", "Faiz A", "1234567890");

        refund.tolakRefund(filed.getId());

        assertFalse(ids(refund.getRefundsAktif()).contains(filed.getId()));
    }

    private static List<String> ids(List<Refund> refunds) {
        return refunds.stream().map(Refund::getId).toList();
    }
}
