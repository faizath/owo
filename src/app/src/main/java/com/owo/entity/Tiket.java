package com.owo.entity;

public abstract class Tiket {
    private int id;
    private float harga;
    private boolean tersedia;

    /**
     * How many people this bookable unit holds — passengers for a flight, guests for a
     * hotel room. One {@code tiket} row is one bookable unit, so a party larger than this
     * cannot be seated by a single booking and the search filters the row out.
     */
    private int kapasitas = 1;

    public Tiket(int id, float harga, boolean tersedia) {
        this.id = id;
        this.harga = harga;
        this.tersedia = tersedia;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getHarga() {
        return harga;
    }

    public void setHarga(float harga) {
        this.harga = harga;
    }

    public boolean isTersedia() {
        return tersedia;
    }

    public void setTersedia(boolean tersedia) {
        this.tersedia = tersedia;
    }

    /**
     * What a party of {@code jumlahPeserta} pays for this unit.
     *
     * <p>{@code harga} alone is not the answer, and what it means differs by ticket type:
     * a hotel room costs the same whether one guest or four sleep in it, while a flight
     * is sold per seat. Charging a party of four the solo price was correct for hotels
     * and wrong for flights, so the rule belongs to the subclass rather than to a caller
     * that has to remember which is which.
     *
     * @throws IllegalArgumentException if the party is not at least one, or exceeds what
     *     this unit holds
     */
    public double hitungTotalHarga(int jumlahPeserta) {
        if (jumlahPeserta < 1) {
            throw new IllegalArgumentException(
                    "Jumlah peserta minimal 1, bukan " + jumlahPeserta);
        }
        if (jumlahPeserta > kapasitas) {
            throw new IllegalArgumentException("Tiket ini hanya memuat " + kapasitas
                    + " orang, tidak cukup untuk " + jumlahPeserta + " orang.");
        }
        return harga * satuanDikenakan(jumlahPeserta);
    }

    /**
     * How many times {@link #getHarga()} is charged for a party of {@code jumlahPeserta}.
     *
     * <p>One by default: the price is for the unit. Overridden where it is per person.
     */
    protected int satuanDikenakan(int jumlahPeserta) {
        return 1;
    }

    public int getKapasitas() {
        return kapasitas;
    }

    /** @throws IllegalArgumentException if the capacity is not at least one */
    public void setKapasitas(int kapasitas) {
        if (kapasitas < 1) {
            throw new IllegalArgumentException("Kapasitas tiket minimal 1, bukan " + kapasitas);
        }
        this.kapasitas = kapasitas;
    }

}
