package com.owotest;

import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TiketTest {

    private TiketHotel tiketHotel;
    private TiketPesawat tiketPesawat;
    private LocalDate checkIn;
    private LocalDate checkOut;

    @BeforeEach
    void setUp() {
        checkIn = LocalDate.of(2024, 3, 15);
        checkOut = LocalDate.of(2024, 3, 20);
        tiketHotel = new TiketHotel(1, 1000000.0f, true, checkIn, checkOut, "Grand Hotel", "101", "Jakarta");
        tiketPesawat = TiketPesawat.builder()
                .id(2)
                .harga(2000000.0f)
                .tersedia(true)
                .flightNumber("GA123")
                .origin("Jakarta")
                .destination("Bali")
                .maskapai("Garuda Indonesia")
                .kelas("Ekonomi")
                .waktuKeberangkatan(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    @DisplayName("Test TiketHotel creation and getters")
    void testTiketHotelCreation() {
        assertEquals(1, tiketHotel.getId());
        assertEquals(1000000.0f, tiketHotel.getHarga());
        assertTrue(tiketHotel.isTersedia());
        assertEquals(checkIn, tiketHotel.getCheckIn());
        assertEquals(checkOut, tiketHotel.getCheckOut());
        assertEquals("Grand Hotel", tiketHotel.getHotelName());
        assertEquals("101", tiketHotel.getRoomNumber());
        assertEquals("Jakarta", tiketHotel.getAddress());
    }

    @Test
    @DisplayName("Test TiketHotel setters")
    void testTiketHotelSetters() {
        LocalDate newCheckIn = LocalDate.of(2024, 4, 1);
        LocalDate newCheckOut = LocalDate.of(2024, 4, 5);
        
        tiketHotel.setCheckIn(newCheckIn);
        tiketHotel.setCheckOut(newCheckOut);
        tiketHotel.setHotelName("New Grand Hotel");
        tiketHotel.setRoomNumber("202");
        tiketHotel.setAddress("Surabaya");
        
        assertEquals(newCheckIn, tiketHotel.getCheckIn());
        assertEquals(newCheckOut, tiketHotel.getCheckOut());
        assertEquals("New Grand Hotel", tiketHotel.getHotelName());
        assertEquals("202", tiketHotel.getRoomNumber());
        assertEquals("Surabaya", tiketHotel.getAddress());
    }

    @Test
    @DisplayName("Test TiketPesawat creation and getters")
    void testTiketPesawatCreation() {
        assertEquals(2, tiketPesawat.getId());
        assertEquals(2000000.0f, tiketPesawat.getHarga());
        assertTrue(tiketPesawat.isTersedia());
        assertEquals("GA123", tiketPesawat.getFlightNumber());
        assertEquals("Garuda Indonesia", tiketPesawat.getMaskapai());
        assertEquals("Jakarta", tiketPesawat.getOrigin());
        assertEquals("Bali", tiketPesawat.getDestination());
        assertEquals("Ekonomi", tiketPesawat.getKelas());
        assertNotNull(tiketPesawat.getWaktuKeberangkatan());
    }

    @Test
    @DisplayName("Test Tiket base class setters")
    void testTiketBaseSetters() {
        // Test setters on TiketHotel
        tiketHotel.setId(3);
        tiketHotel.setHarga(1500000.0f);
        tiketHotel.setTersedia(false);

        assertEquals(3, tiketHotel.getId());
        assertEquals(1500000.0f, tiketHotel.getHarga());
        assertFalse(tiketHotel.isTersedia());

        // Test setters on TiketPesawat
        tiketPesawat.setId(4);
        tiketPesawat.setHarga(2500000.0f);
        tiketPesawat.setTersedia(false);

        assertEquals(4, tiketPesawat.getId());
        assertEquals(2500000.0f, tiketPesawat.getHarga());
        assertFalse(tiketPesawat.isTersedia());
    }
}
