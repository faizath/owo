package com.owo.entity;

import java.time.LocalDate;

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
}
