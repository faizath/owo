package com.owotest;

import com.owo.controller.PemesananController;
import com.owo.controller.RefundController;
import com.owo.dao.AkunDAO;
import com.owo.dao.RefundDAO;
import com.owo.entity.Akun;
import com.owo.entity.Pemesanan;
import com.owo.entity.PemesananStatus;
import com.owo.entity.Refund;
import com.owo.utils.JavaScriptBridge;
import com.owotest.support.Fixtures;
import com.owotest.support.TempDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the authorization claims this codebase makes about itself.
 *
 * <p>Each of these is a property that is easy to state in a comment and easy to break
 * later without any existing test noticing. They are asserted here so the claim fails
 * loudly rather than quietly becoming untrue.
 */
class SecurityClaimsTest {

    private TempDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        db = new TempDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    // --------------------------------------------------- administrator escalation

    @Test
    void savingAnAccountCannotGrantItAdministratorRights() throws Exception {
        Akun customer = AkunDAO.createAkun("Biasa", "biasa@example.com", "password123");
        assertFalse(customer.isAdmin());

        // The only mutation path a signed-in user's data can take. If it ever started
        // writing is_admin, a profile save would become a privilege escalation.
        customer.setNama("Biasa Diubah");
        AkunDAO.updateAkun(customer);

        assertFalse(AkunDAO.getAkunByID(customer.getID()).isAdmin(),
                "updateAkun granted administrator rights");
    }

    @Test
    void savingAnAdministratorDoesNotStripItsRights() throws Exception {
        Akun admin = AkunDAO.createAkun("Admin", "admin@example.com", "password123", true);

        admin.setNama("Admin Diubah");
        AkunDAO.updateAkun(admin);

        // The mirror of the previous case: leaving is_admin out of the UPDATE must not be
        // read as "set it to false".
        assertTrue(AkunDAO.getAkunByID(admin.getID()).isAdmin(),
                "updateAkun revoked administrator rights");
    }

    @Test
    void selfServiceRegistrationNeverProducesAnAdministrator() throws Exception {
        Akun akun = com.owo.controller.AuthController.register(
                "Pendaftar", "pendaftar@example.com", "password123");

        assertFalse(akun.isAdmin());
        assertFalse(AkunDAO.getAkunByEmail("pendaftar@example.com").isAdmin());
    }

    // ------------------------------------------------------------ bridge contract

    @Test
    void noBridgeMethodAcceptsAnAdministratorFlag() {
        // Identity and authority both come from the session. A boolean parameter on a
        // review method would let the page assert its own authority.
        List<String> offenders = new ArrayList<>();
        for (Method method : JavaScriptBridge.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            for (Class<?> type : method.getParameterTypes()) {
                if (type == boolean.class || type == Boolean.class) {
                    offenders.add(method.getName());
                }
            }
        }
        assertTrue(offenders.isEmpty(), "bridge methods taking a boolean: " + offenders);
    }

    @Test
    void everyPublicBridgeMethodIsCallableFromJavaScript() {
        // WebView marshals only String and numerics; anything else is unreachable from the
        // page and would be dead on arrival, which is how the original bridge shipped.
        List<String> offenders = new ArrayList<>();
        for (Method method : JavaScriptBridge.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                continue;
            }
            for (Class<?> type : method.getParameterTypes()) {
                if (type != String.class && !type.isPrimitive()) {
                    offenders.add(method.getName() + " takes " + type.getSimpleName());
                }
            }
        }
        assertTrue(offenders.isEmpty(), String.join(", ", offenders));
    }

    // ------------------------------------------------------ cross-user refund access

    @Test
    void aRefundCannotBeReadByAnotherCustomer() throws Exception {
        Akun owner = Fixtures.customer();
        Akun stranger = Fixtures.customer();

        Pemesanan booking = Fixtures.booking(owner, Fixtures.flight(20),
                PemesananStatus.CONFIRMED);
        RefundController refunds = new RefundController();
        Refund refund = refunds.ajukanRefund(booking.getId(), owner.getID(),
                "Perubahan rencana", "Penerima", "1234567890");

        assertThrows(PemesananController.PemesananException.class,
                () -> refunds.getOwnedRefund(refund.getId(), stranger.getID()));

        // The listing is scoped in SQL, not filtered afterwards.
        assertTrue(refunds.getRefundsForCustomer(stranger.getID()).isEmpty(),
                "a stranger's refund listing was not empty");
        assertEquals(1, refunds.getRefundsForCustomer(owner.getID()).size());
    }

    @Test
    void aMissingRefundAndAForeignOneAreIndistinguishable() throws Exception {
        Akun owner = Fixtures.customer();
        Akun stranger = Fixtures.customer();

        Pemesanan booking = Fixtures.booking(owner, Fixtures.flight(20),
                PemesananStatus.CONFIRMED);
        RefundController refunds = new RefundController();
        Refund refund = refunds.ajukanRefund(booking.getId(), owner.getID(),
                "Perubahan rencana", "Penerima", "1234567890");

        String foreign = assertThrows(PemesananController.PemesananException.class,
                () -> refunds.getOwnedRefund(refund.getId(), stranger.getID())).getMessage();
        String missing = assertThrows(PemesananController.PemesananException.class,
                () -> refunds.getOwnedRefund("RFD-NOPE", stranger.getID())).getMessage();

        // Any difference here is enough to walk the id space and learn which ids are real.
        assertEquals(missing, foreign);
    }

    @Test
    void aStrangerCannotRewriteWhereARefundPaysOut() throws Exception {
        Akun owner = Fixtures.customer();
        Akun stranger = Fixtures.customer();

        Pemesanan booking = Fixtures.booking(owner, Fixtures.flight(20),
                PemesananStatus.CONFIRMED);
        RefundController refunds = new RefundController();
        Refund refund = refunds.ajukanRefund(booking.getId(), owner.getID(),
                "Perubahan rencana", "Pemilik", "1111111111");

        assertThrows(PemesananController.PemesananException.class,
                () -> refunds.perbaruiDetailPencairan(
                        refund.getId(), stranger.getID(), "Penyerang", "9999999999"));

        // Redirecting a payout is the whole prize here, so assert the row itself.
        Refund stored = RefundDAO.getRefundById(refund.getId());
        assertEquals("Pemilik", stored.getNamaPenerima());
        assertEquals("1111111111", stored.getRekeningTujuan());
    }

    @Test
    void aDecidedRefundsPayeeIsFrozen() throws Exception {
        Akun owner = Fixtures.customer();
        Pemesanan booking = Fixtures.booking(owner, Fixtures.flight(20),
                PemesananStatus.CONFIRMED);
        RefundController refunds = new RefundController();
        Refund refund = refunds.ajukanRefund(booking.getId(), owner.getID(),
                "Perubahan rencana", "Pemilik", "1111111111");

        refunds.setujuiRefund(refund.getId(), Fixtures.admin().getID());

        // After approval the payee is part of the settled record; changing it then would
        // redirect money that has already been signed off.
        assertThrows(PemesananController.PemesananException.class,
                () -> refunds.perbaruiDetailPencairan(
                        refund.getId(), owner.getID(), "Lain", "2222222222"));
    }
}
