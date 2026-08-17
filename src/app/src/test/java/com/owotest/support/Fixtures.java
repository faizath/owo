package com.owotest.support;

import com.owo.dao.AkunDAO;
import com.owo.dao.PemesananDAO;
import com.owo.dao.TiketDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.TiketHotel;
import com.owo.entity.TiketPesawat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Known records for controller and DAO tests.
 *
 * <p>All dates are relative to now. Hardcoded dates are what silently expired the seed
 * data, and the same trap applies to fixtures.
 */
public final class Fixtures {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private Fixtures() {
    }

    public static Akun customer() throws Exception {
        int n = COUNTER.incrementAndGet();
        return AkunDAO.createAkun("Pengguna " + n, "user" + n + "@example.com", "password123");
    }

    /** An account flagged as an administrator, which only seeding can otherwise produce. */
    public static Akun admin() throws Exception {
        int n = COUNTER.incrementAndGet();
        return AkunDAO.createAkun("Admin " + n, "admin" + n + "@example.com", "password123", true);
    }

    /** A flight seating {@code kapasitas} people, for capacity filtering. */
    public static TiketPesawat flightSeating(int daysAhead, int kapasitas) throws Exception {
        int n = COUNTER.incrementAndGet();
        return TiketDAO.createTiketPesawat(1_500_000f, true, "GA" + (400 + n),
                "Jakarta (CGK)", "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi",
                LocalDate.now().plusDays(daysAhead).atTime(9, 0), kapasitas);
    }

    /** A hotel room holding {@code kapasitas} guests. */
    public static TiketHotel hotelSeating(int checkInDaysAhead, int kapasitas) throws Exception {
        int n = COUNTER.incrementAndGet();
        return TiketDAO.createTiketHotel(750_000f, true,
                LocalDate.now().plusDays(checkInDaysAhead),
                LocalDate.now().plusDays(checkInDaysAhead + 3),
                "Hotel " + n, "10" + n, "Ubud, Bali", kapasitas);
    }

    /** A flight departing {@code daysAhead} days from now at 09:00. */
    public static TiketPesawat flight(int daysAhead) throws Exception {
        return flight(daysAhead, 1_500_000f);
    }

    public static TiketPesawat flight(int daysAhead, float harga) throws Exception {
        int n = COUNTER.incrementAndGet();
        return TiketDAO.createTiketPesawat(harga, true, "GA" + (400 + n),
                "Jakarta (CGK)", "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi",
                LocalDate.now().plusDays(daysAhead).atTime(9, 0));
    }

    /** A flight departing at a precise moment, for tier-boundary cases. */
    public static TiketPesawat flightAt(LocalDateTime departure, float harga) throws Exception {
        int n = COUNTER.incrementAndGet();
        return TiketDAO.createTiketPesawat(harga, true, "GA" + (400 + n),
                "Jakarta (CGK)", "Denpasar (DPS)", "Garuda Indonesia", "Ekonomi", departure);
    }

    public static TiketHotel hotel(int checkInDaysAhead) throws Exception {
        int n = COUNTER.incrementAndGet();
        return TiketDAO.createTiketHotel(750_000f, true,
                LocalDate.now().plusDays(checkInDaysAhead),
                LocalDate.now().plusDays(checkInDaysAhead + 3),
                "Hotel " + n, "10" + n, "Ubud, Bali");
    }

    /** A booking in the requested status, persisted. */
    public static Pemesanan booking(Akun customer, com.owo.entity.Tiket tiket,
            PemesananStatus status) throws Exception {
        Pemesanan pemesanan = PemesananDAO.createPemesanan(customer.getID(), tiket);
        if (status != PemesananStatus.PENDING) {
            PemesananDAO.updateStatus(pemesanan.getId(), status);
            pemesanan.setStatus(status.dbValue());
        }
        return pemesanan;
    }
}
