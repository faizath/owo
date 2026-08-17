package com.owo.entity;

import java.time.LocalDate;

/**
 * A bookable hotel room.
 *
 * <p>Priced per unit: the rate is the same whether one guest or four sleep in it, so this
 * keeps {@link Tiket}'s default of charging {@code harga} once rather than per guest.
 */
public class TiketHotel extends Tiket {
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String hotelName;
    private String roomNumber;
    private String address;

    public TiketHotel(int id, float harga, boolean tersedia, LocalDate checkIn, LocalDate checkOut, String hotelName,
            String roomNumber, String address) {
        super(id, harga, tersedia);
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.hotelName = hotelName;
        this.roomNumber = roomNumber;
        this.address = address;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public String getHotelName() {
        return hotelName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}