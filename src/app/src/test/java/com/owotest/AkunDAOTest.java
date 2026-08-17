package com.owotest;

import com.owo.controller.AuthController;
import com.owo.dao.AkunDAO;
import com.owo.entity.Akun;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AkunDAOTest {

    private static final String PASSWORD = "password123";

    private TempDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void createThenFetchByEmail_passwordVerifies() throws Exception {
        AkunDAO.createAkun("Budi", "budi@example.com", PASSWORD);

        Akun fetched = AkunDAO.getAkunByEmail("budi@example.com");

        assertNotNull(fetched);
        // Hydration used to pass the stored hash into the plaintext constructor
        // parameter, producing bcrypt(bcrypt(password)).
        assertTrue(fetched.checkPassword(PASSWORD), "stored password did not verify");
        assertFalse(fetched.checkPassword("wrong-password"));
    }

    @Test
    void createThenFetchById_passwordVerifies() throws Exception {
        Akun created = AkunDAO.createAkun("Siti", "siti@example.com", PASSWORD);

        Akun fetched = AkunDAO.getAkunByID(created.getID());

        assertNotNull(fetched);
        assertTrue(fetched.checkPassword(PASSWORD), "stored password did not verify");
    }

    @Test
    void createdEntity_carriesTheStoredHash() throws Exception {
        Akun created = AkunDAO.createAkun("Andi", "andi@example.com", PASSWORD);

        Akun fetched = AkunDAO.getAkunByEmail("andi@example.com");

        assertNotNull(fetched);
        assertEquals(created.getHashedPassword(), fetched.getHashedPassword());
    }

    @Test
    void registerDuplicateEmail_isRefused() throws Exception {
        AuthController.register("Budi", "dup@example.com", PASSWORD);

        assertThrows(AuthController.AuthException.class,
                () -> AuthController.register("Budi Lain", "dup@example.com", PASSWORD));
    }

    @Test
    void login_survivesHydrationFromTheDatabase() throws Exception {
        AuthController.register("Dewi", "dewi@example.com", PASSWORD);

        // Nothing holds this account in memory; login must read it back from SQLite.
        Akun loggedIn = AuthController.login("dewi@example.com", PASSWORD);

        assertNotNull(loggedIn);
        assertEquals("Dewi", loggedIn.getNama());
    }

    @Test
    void login_withWrongPassword_isRefused() throws Exception {
        AuthController.register("Eko", "eko@example.com", PASSWORD);

        assertThrows(AuthController.AuthException.class,
                () -> AuthController.login("eko@example.com", "not-the-password"));
    }

    @Test
    void login_withUnknownEmail_reportsTheSameMessageAsAWrongPassword() throws Exception {
        AuthController.register("Fajar", "fajar@example.com", PASSWORD);

        AuthController.AuthException unknownEmail = assertThrows(AuthController.AuthException.class,
                () -> AuthController.login("nobody@example.com", PASSWORD));
        AuthController.AuthException wrongPassword = assertThrows(AuthController.AuthException.class,
                () -> AuthController.login("fajar@example.com", "wrong"));

        // Distinct messages would let an attacker enumerate registered accounts.
        assertEquals(unknownEmail.getMessage(), wrongPassword.getMessage());
    }

    @Test
    void register_withShortPassword_isRefused() {
        assertThrows(AuthController.AuthException.class,
                () -> AuthController.register("Gita", "gita@example.com", "short"));
    }

    @Test
    void register_withMalformedEmail_isRefused() {
        assertThrows(AuthController.AuthException.class,
                () -> AuthController.register("Hadi", "not-an-email", PASSWORD));
    }
}
