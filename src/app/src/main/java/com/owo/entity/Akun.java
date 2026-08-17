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
     *
     * <p>Authority is not optional here. The overload that omitted it silently produced an
     * ordinary account, so hydrating an administrator through the shorter form demoted
     * them with nothing to notice — a caller must say what the row said.
     */
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

    /**
     * Sets a new password from plaintext, hashing it.
     *
     * <p>Named for what it takes, not for what it stores: as {@code setHashedPassword} it
     * read as though it accepted an already-hashed value, and handing it one would have
     * stored {@code bcrypt(bcrypt(password))} against which nothing can authenticate.
     */
    public void setPassword(String password) {
        this.hashedPassword = passwordUtil.hashPassword(password);
    }

    public boolean checkPassword(String password) {
        return passwordUtil.checkPassword(password, hashedPassword);
    }

    public boolean checkEmail(String email) {
        return this.email.equals(email);
    }
}
