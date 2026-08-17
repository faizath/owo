package com.owotest;

import com.owo.controller.PemesananController;
import com.owo.controller.RefundController;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * What a party of more than one actually pays, and gets back.
 *
 * <p>{@code harga} was charged once regardless of party size and {@code jumlahPeserta}
 * scaled nothing, so four passengers flew for the price of one and a refund returned a
 * single fare. That is right for a hotel room, which costs the same however many guests
 * sleep in it, and wrong for a flight, which is sold per seat.
 */
class HargaPesertaTest {

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

    @Test
    void aFlightIsChargedPerPassenger() throws Exception {
        TiketPesawat flight = Fixtures.flightSeating(30, 4);

        assertEquals(flight.getHarga() * 4, flight.hitungTotalHarga(4), 0.01);
        assertEquals(flight.getHarga(), flight.hitungTotalHarga(1), 0.01);
    }

    @Test
    void aHotelRoomIsChargedPerRoom() throws Exception {
        TiketHotel room = Fixtures.hotelSeating(30, 4);

        // One room, four guests: the rate does not multiply.
        assertEquals(room.getHarga(), room.hitungTotalHarga(4), 0.01);
    }

    @Test
    void aPartyLargerThanTheUnitHoldsHasNoPrice() throws Exception {
        TiketPesawat flight = Fixtures.flightSeating(30, 2);

        assertThrows(IllegalArgumentException.class, () -> flight.hitungTotalHarga(3));
        assertThrows(IllegalArgumentException.class, () -> flight.hitungTotalHarga(0));
    }

    @Test
    void aRefundOnAFlightReturnsWhatThePartyPaid() throws Exception {
        TiketPesawat flight = Fixtures.flightSeating(30, 4);
        Pemesanan booking = pemesanan.createPemesanan(customer.getID(), flight, 4);
        pemesanan.transition(booking, PemesananStatus.CONFIRMED);

        double actual = refund.hitungJumlahRefund(booking);

        // More than seven days out, so the higher tier applies to all four fares.
        double expected = (flight.getHarga() * 4 * RefundController.TIER_AWAL)
                - RefundController.BIAYA_ADMIN;
        assertEquals(expected, actual, 0.01);
    }

    @Test
    void aRefundOnAHotelRoomDoesNotMultiplyByGuests() throws Exception {
        TiketHotel room = Fixtures.hotelSeating(30, 4);
        Pemesanan booking = pemesanan.createPemesanan(customer.getID(), room, 4);
        pemesanan.transition(booking, PemesananStatus.CONFIRMED);

        double expected = (room.getHarga() * RefundController.TIER_AWAL)
                - RefundController.BIAYA_ADMIN;
        assertEquals(expected, refund.hitungJumlahRefund(booking), 0.01);
    }
}
