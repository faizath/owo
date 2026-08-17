package com.owotest;

import com.owo.controller.AuthController;
import com.owo.dao.AkunDAO;
import com.owo.entity.Akun;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Editing the signed-in account.
 *
 * <p>"Pengaturan Akun" was a sidebar entry whose handler called preventDefault and
 * returned. Backing it means the profile becomes writable, which is why the password
 * change is the interesting case rather than the name.
 */
class PengaturanAkunTest {

    private TempDatabase db;
    private Akun customer;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
        customer = Fixtures.customer();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void gantiNama_persistsTheNewName() throws Exception {
        AuthController.gantiNama(customer.getID(), "  Budi Santoso  ");

        assertEquals("Budi Santoso", AkunDAO.getAkunByID(customer.getID()).getNama());
    }

    @Test
    void gantiNama_withABlankName_isRefused() throws Exception {
        assertThrows(AuthController.AuthException.class,
                () -> AuthController.gantiNama(customer.getID(), "   "));

        assertEquals(customer.getNama(), AkunDAO.getAkunByID(customer.getID()).getNama());
    }

    @Test
    void gantiNama_doesNotDisturbTheStoredPassword() throws Exception {
        AuthController.gantiNama(customer.getID(), "Budi Santoso");

        // updateAkun writes the whole row. Re-hashing an already-hashed password here
        // would lock the account out of its own credentials without any visible symptom.
        assertTrue(AkunDAO.getAkunByID(customer.getID()).checkPassword("password123"));
    }

    @Test
    void gantiNama_cannotPromoteToAdministrator() throws Exception {
        Akun admin = Fixtures.admin();

        AuthController.gantiNama(customer.getID(), "Budi Santoso");
        AuthController.gantiNama(admin.getID(), "Admin Baru");

        // updateAkun deliberately does not write is_admin, so no path that hydrates an
        // account and writes it back can change authority.
        assertFalse(AkunDAO.getAkunByID(customer.getID()).isAdmin());
        assertTrue(AkunDAO.getAkunByID(admin.getID()).isAdmin());
    }

    @Test
    void gantiPassword_replacesTheCredential() throws Exception {
        AuthController.gantiPassword(customer.getID(), "password123", "kataSandiBaru9");

        Akun stored = AkunDAO.getAkunByID(customer.getID());
        assertTrue(stored.checkPassword("kataSandiBaru9"));
        assertFalse(stored.checkPassword("password123"));
    }

    @Test
    void gantiPassword_withoutTheCurrentOne_isRefused() throws Exception {
        // An unattended session would otherwise be enough to lock its owner out.
        assertThrows(AuthController.AuthException.class,
                () -> AuthController.gantiPassword(customer.getID(), "salah", "kataSandiBaru9"));

        assertTrue(AkunDAO.getAkunByID(customer.getID()).checkPassword("password123"));
    }

    @Test
    void gantiPassword_withAShortNewPassword_isRefused() throws Exception {
        assertThrows(AuthController.AuthException.class,
                () -> AuthController.gantiPassword(customer.getID(), "password123", "pendek"));

        assertTrue(AkunDAO.getAkunByID(customer.getID()).checkPassword("password123"));
    }

    @Test
    void gantiPassword_withTheSamePassword_isRefused() throws Exception {
        assertThrows(AuthController.AuthException.class,
                () -> AuthController.gantiPassword(customer.getID(), "password123", "password123"));
    }

    @Test
    void aChangedPasswordIsWhatLoginAccepts() throws Exception {
        AuthController.gantiPassword(customer.getID(), "password123", "kataSandiBaru9");

        // The whole point of the screen: the new credential has to work at the front door,
        // not merely satisfy checkPassword on a hydrated entity.
        assertEquals(customer.getID(),
                AuthController.login(customer.getEmail(), "kataSandiBaru9").getID());
        assertThrows(AuthController.AuthException.class,
                () -> AuthController.login(customer.getEmail(), "password123"));
    }
}
