package com.owo.entity;

import com.owo.utils.PasswordUtil;

public class Akun {
    private static PasswordUtil passwordUtil = new PasswordUtil();
    private int ID;
    private String nama, email, hashedPassword;

    /**
     * Whether this account may review refunds. Held in Java and read from the row; the
     * page never asserts it, so a client cannot promote itself.
     */
    private final boolean admin;

    /** Takes a plaintext password and hashes it. Use for accounts being created. */
    public Akun(int ID, String nama, String email, String password) {
        this.ID = ID;
        this.nama = nama;
        this.email = email;
        this.hashedPassword = passwordUtil.hashPassword(password);
        this.admin = false;
    }

    private Akun(int ID, String nama, String email, String hashedPassword, boolean admin,
            boolean alreadyHashed) {
        this.ID = ID;
        this.nama = nama;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.admin = admin;
    }

    /**
     * Rebuilds an account from a stored hash, without hashing it a second time.
     *
     * <p>Passing a stored hash to the public constructor produces
     * {@code bcrypt(bcrypt(password))}, against which {@link #checkPassword} can never
     * succeed. DAO hydration must use this factory.
     */
    public static Akun fromHashedPassword(int ID, String nama, String email, String hashedPassword) {
        return new Akun(ID, nama, email, hashedPassword, false, true);
    }

    /** As {@link #fromHashedPassword}, carrying the stored administrator flag. */
    public static Akun fromHashedPassword(int ID, String nama, String email, String hashedPassword,
            boolean admin) {
        return new Akun(ID, nama, email, hashedPassword, admin, true);
    }

    public boolean isAdmin() {
        return admin;
    }

    public int getID() {
        return ID;
    }

    public String getNama() {
        return nama;
    }

    public String getEmail() {
        return email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setHashedPassword(String password) {
        this.hashedPassword = passwordUtil.hashPassword(password);
    }

    public boolean checkPassword(String password) {
        return passwordUtil.checkPassword(password, hashedPassword);
    }

    public boolean checkEmail(String email) {
        return this.email.equals(email);
    }
}
