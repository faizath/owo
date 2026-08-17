package com.owo.controller;

import com.owo.dao.AkunDAO;
import com.owo.entity.Akun;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Authentication against the {@code akun} table.
 *
 * <p>This used to search a static in-memory list populated as a side effect of entity
 * construction, so an account could never be found after a restart.
 */
public class AuthController {

    /** Deliberately permissive: enough to reject obvious typos, not to police valid addresses. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public static final int MIN_PASSWORD_LENGTH = 8;

    /** Raised for any credential problem, always with the same message. */
    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }

    /**
     * @return the authenticated account
     * @throws AuthException if the email is unknown or the password does not match. The
     *     message is identical in both cases so it cannot be used to enumerate accounts.
     */
    public static Akun login(String email, String password) throws AuthException, SQLException {
        String normalisedEmail = normaliseEmail(email);
        if (normalisedEmail.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthException("Email atau password salah.");
        }

        Akun akun = AkunDAO.getAkunByEmail(normalisedEmail);
        if (akun == null || !akun.checkPassword(password)) {
            throw new AuthException("Email atau password salah.");
        }
        return akun;
    }

    /**
     * @return the newly created account
     * @throws AuthException if the input fails validation or the email is already taken
     */
    public static Akun register(String nama, String email, String password)
            throws AuthException, SQLException {
        if (nama == null || nama.trim().isEmpty()) {
            throw new AuthException("Nama tidak boleh kosong.");
        }

        String normalisedEmail = normaliseEmail(email);
        if (!EMAIL_PATTERN.matcher(normalisedEmail).matches()) {
            throw new AuthException("Format email tidak valid.");
        }

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException(
                    "Password minimal " + MIN_PASSWORD_LENGTH + " karakter.");
        }

        if (AkunDAO.getAkunByEmail(normalisedEmail) != null) {
            throw new AuthException("Email sudah terdaftar.");
        }

        return AkunDAO.createAkun(nama.trim(), normalisedEmail, password);
    }

    private static String normaliseEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
