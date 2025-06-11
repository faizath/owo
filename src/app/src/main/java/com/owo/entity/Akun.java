package com.owo.entity;

import com.owo.utils.PasswordUtil;
import java.util.ArrayList;

public class Akun {
    private static PasswordUtil passwordUtil = new PasswordUtil();
    private static ArrayList<Akun> allAkun = new ArrayList<>();
    private int ID;
    private String nama, email, hashedPassword;

    public Akun(String nama, String email, String password) {
        this.nama = nama;
        this.email = email;
        this.hashedPassword = passwordUtil.hashPassword(password);
        this.ID = allAkun.size() + 1;
        allAkun.add(this);
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