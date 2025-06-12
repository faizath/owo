package com.owo.controller;

import com.owo.entity.Akun;

public class AuthController {
    public static Akun login(String email, String password) {
        Akun akun = Akun.getAkunByEmail(email);
        if (akun != null && akun.checkPassword(password)) {
            return akun;
        }
        return null;
    }
}
