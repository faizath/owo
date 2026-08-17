package com.owo.entity;

import com.owo.utils.PasswordUtil;

public class Akun {
    private static PasswordUtil passwordUtil = new PasswordUtil();
    private int ID;
    private String nama, email, hashedPassword;

    /** Takes a plaintext password and hashes it. Use for accounts being created. */
    public Akun(int ID, String nama, String email, String password) {
        this.ID = ID;
        this.nama = nama;
        this.email = email;
        this.hashedPassword = passwordUtil.hashPassword(password);
    }

    private Akun(int ID, String nama, String email, String hashedPassword, boolean alreadyHashed) {
        this.ID = ID;
        this.nama = nama;
        this.email = email;
        this.hashedPassword = hashedPassword;
    }

    /**
     * Rebuilds an account from a stored hash, without hashing it a second time.
     *
     * <p>Passing a stored hash to the public constructor produces
     * {@code bcrypt(bcrypt(password))}, against which {@link #checkPassword} can never
     * succeed. DAO hydration must use this factory.
     */
    public static Akun fromHashedPassword(int ID, String nama, String email, String hashedPassword) {
        return new Akun(ID, nama, email, hashedPassword, true);
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
