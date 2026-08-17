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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The administrator flag on {@code akun}.
 *
 * <p>It decides who may review refunds, so the interesting property is not that it can be
 * set but that it can only be set by seeding: no self-service path, and no write-back of a
 * hydrated account, may change it.
 */
class AkunAdminTest {

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
    void anAccountCreatedWithoutTheFlag_isNotAnAdministrator() throws Exception {
        AkunDAO.createAkun("Budi", "budi@example.com", PASSWORD);

        assertFalse(AkunDAO.getAkunByEmail("budi@example.com").isAdmin());
    }

    @Test
    void aSelfRegisteredAccount_isNotAnAdministrator() throws Exception {
        AuthController.register("Siti", "siti@example.com", PASSWORD);

        // Registration is reachable from the page. If it could produce an administrator,
        // the review queue would be open to anyone.
        assertFalse(AkunDAO.getAkunByEmail("siti@example.com").isAdmin());
    }

    @Test
    void anAdministratorAccount_isReadBackAsAdministrator() throws Exception {
        AkunDAO.createAkun("Admin", "admin@example.com", PASSWORD, true);

        Akun byEmail = AkunDAO.getAkunByEmail("admin@example.com");
        Akun byId = AkunDAO.getAkunByID(byEmail.getID());

        // Both hydration paths read the column; one that forgot it would silently demote
        // the administrator on whichever screen used it.
        assertTrue(byEmail.isAdmin());
        assertTrue(byId.isAdmin());
    }

    @Test
    void createAkun_returnsTheFlagItJustWrote() throws Exception {
        Akun created = AkunDAO.createAkun("Admin", "admin@example.com", PASSWORD, true);

        assertTrue(created.isAdmin());
    }

    @Test
    void anAdministratorLogsInAsAnAdministrator() throws Exception {
        AkunDAO.createAkun("Admin", "admin@example.com", PASSWORD, true);

        Akun loggedIn = AuthController.login("admin@example.com", PASSWORD);

        assertNotNull(loggedIn);
        assertTrue(loggedIn.isAdmin());
    }

    @Test
    void fromHashedPassword_withoutTheFlag_buildsANonAdministrator() {
        // The four-argument factory predates the flag; it must not default to true, and
        // callers that have not been updated must produce ordinary accounts.
        assertFalse(Akun.fromHashedPassword(1, "Budi", "budi@example.com", "hash").isAdmin());
        assertTrue(Akun.fromHashedPassword(1, "Admin", "admin@example.com", "hash", true).isAdmin());
        assertFalse(Akun.fromHashedPassword(1, "Budi", "budi@example.com", "hash", false).isAdmin());
    }

    @Test
    void updateAkun_doesNotDemoteAnAdministrator() throws Exception {
        Akun admin = AkunDAO.createAkun("Admin", "admin@example.com", PASSWORD, true);

        admin.setNama("Admin Baru");
        AkunDAO.updateAkun(admin);

        // updateAkun deliberately leaves is_admin alone. Including it in the UPDATE would
        // make every profile save rewrite the privilege from whatever the in-memory copy
        // happened to hold.
        Akun reread = AkunDAO.getAkunByID(admin.getID());
        assertEquals("Admin Baru", reread.getNama());
        assertTrue(reread.isAdmin(), "a profile update demoted an administrator");
    }

    @Test
    void updateAkun_cannotPromoteAnOrdinaryAccount() throws Exception {
        AkunDAO.createAkun("Budi", "budi@example.com", PASSWORD);
        int id = AkunDAO.getAkunByEmail("budi@example.com").getID();

        // The only in-memory account that claims to be an administrator is one built by
        // hand — which is exactly the shape an escalation attempt would take.
        Akun forged = Akun.fromHashedPassword(id, "Budi", "budi@example.com",
                AkunDAO.getAkunByID(id).getHashedPassword(), true);
        AkunDAO.updateAkun(forged);

        assertFalse(AkunDAO.getAkunByID(id).isAdmin(),
                "writing back a hydrated account escalated it to administrator");
    }
}
