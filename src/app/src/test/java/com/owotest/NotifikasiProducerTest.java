package com.owotest;

import com.owo.controller.CheckInController;
import com.owo.controller.PemesananController;
import com.owo.controller.RefundController;
import com.owo.dao.NotifikasiDAO;
import com.owo.entity.Akun;
import com.owo.entity.Notifikasi;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.Refund;
import com.owo.entity.TiketPesawat;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every notification the system produces.
 *
 * <p>The polling loop, the retry logic, the delivery bridge and the toast rendering were
 * all working and all read a table nothing ever wrote to: {@code createNotifikasi} had no
 * caller anywhere in {@code main}. A test that only exercised the DAO directly would have
 * stayed green through all of that, so each case here drives a real operation and then
 * asks what the user would actually be told.
 */
class NotifikasiProducerTest {

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

    private List<Notifikasi> inbox(Akun akun) throws Exception {
        return NotifikasiDAO.getNotifikasiByUserId(akun.getID());
    }

    private void assertMentions(Akun akun, String fragment) throws Exception {
        List<Notifikasi> messages = inbox(akun);
        assertTrue(messages.stream().anyMatch(n -> n.getPesan().contains(fragment)),
                "No notification mentioning '" + fragment + "' in " + messages.stream()
                        .map(Notifikasi::getPesan).toList());
    }

    @Test
    void bookingProducesANotification() throws Exception {
        Pemesanan booking = pemesanan.createPemesanan(customer.getID(), Fixtures.flight(10));

        assertMentions(customer, booking.getKodeBooking());
    }

    @Test
    void paymentProducesANotification() throws Exception {
        Pemesanan booking = pemesanan.createPemesanan(customer.getID(), Fixtures.flight(10));

        pemesanan.konfirmasiPemesanan(booking.getId(), customer.getID());

        assertMentions(customer, "Pembayaran pemesanan " + booking.getKodeBooking());
    }

    @Test
    void cancellationProducesANotification() throws Exception {
        Pemesanan booking = pemesanan.createPemesanan(customer.getID(), Fixtures.flight(10));

        pemesanan.batalkanPemesanan(booking.getId(), customer.getID());

        assertMentions(customer, "dibatalkan");
    }

    @Test
    void checkInProducesANotification() throws Exception {
        // Check-in is same-day, so the ticket has to depart today.
        TiketPesawat flight = Fixtures.flight(0);
        Pemesanan booking = Fixtures.booking(customer, flight, PemesananStatus.CONFIRMED);

        new CheckInController(pemesanan).checkIn(booking.getId(), customer.getID());

        assertMentions(customer, "Check-in untuk pemesanan " + booking.getKodeBooking());
    }

    @Test
    void filingARefundProducesANotification() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(30),
                PemesananStatus.CONFIRMED);

        Refund filed = refund.ajukanRefund(booking.getId(), customer.getID(),
                "Berhalangan hadir", "Faiz A", "1234567890");

        assertMentions(customer, filed.getId());
    }

    @Test
    void approvingARefundNotifiesTheBookingOwnerRatherThanTheReviewer() throws Exception {
        Akun reviewer = Fixtures.admin();
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(30),
                PemesananStatus.CONFIRMED);
        Refund filed = refund.ajukanRefund(booking.getId(), customer.getID(),
                "Berhalangan hadir", "Faiz A", "1234567890");

        refund.setujuiRefund(filed.getId());

        assertMentions(customer, "disetujui");
        // The decision is taken by an administrator; sending the outcome to the session
        // user would tell the wrong person and tell the customer nothing.
        assertEquals(List.of(), inbox(reviewer));
    }

    @Test
    void rejectingARefundNotifiesTheCustomer() throws Exception {
        Pemesanan booking = Fixtures.booking(customer, Fixtures.flight(30),
                PemesananStatus.CONFIRMED);
        Refund filed = refund.ajukanRefund(booking.getId(), customer.getID(),
                "Berhalangan hadir", "Faiz A", "1234567890");

        refund.tolakRefund(filed.getId());

        assertMentions(customer, "ditolak");
    }
}
