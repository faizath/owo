package com.owotest;

import com.owo.entity.Refund;
import com.owo.entity.Refund.RefundStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RefundTest {
    private Refund refund;

    @BeforeEach
    void setUp() {
        refund = new Refund("REF001", 123, "Product damaged", 100.0);
    }

    @Test
    void testConstructor() {
        assertEquals("REF001", refund.getId());
        assertEquals(123, refund.getPemesananID());
        assertEquals("Product damaged", refund.getAlasan());
        assertEquals(100.0, refund.getJumlahRefund());
        assertEquals(RefundStatus.PENDING_REVIEW, refund.getStatus());
    }

    @Test
    void testGetters() {
        assertEquals("REF001", refund.getId());
        assertEquals(123, refund.getPemesananID());
        assertEquals("Product damaged", refund.getAlasan());
        assertEquals(100.0, refund.getJumlahRefund());
        assertEquals(RefundStatus.PENDING_REVIEW, refund.getStatus());
    }

    @Test
    void testSetStatus() {
        refund.setStatus(RefundStatus.APPROVED);
        assertEquals(RefundStatus.APPROVED, refund.getStatus());

        refund.setStatus(RefundStatus.COMPLETED);
        assertEquals(RefundStatus.COMPLETED, refund.getStatus());
    }

    @Test
    void testSetDetailKartu() {
        String namaKartu = "John Doe";
        String nomorKartu = "1234567890123456";
        String expiryMonth = "12";
        String expiryYear = "2025";
        String cvv = "123";

        refund.setDetailKartu(namaKartu, nomorKartu, expiryMonth, expiryYear, cvv);

        String[] details = refund.getDetailKartu();
        assertEquals(namaKartu, details[0]);
        assertEquals(nomorKartu, details[1]);
        assertEquals(expiryMonth, details[2]);
        assertEquals(expiryYear, details[3]);
        assertEquals(cvv, details[4]);
    }

    @Test
    void testGetData() {
        String expectedData = "Refund ID: REF001, Pemesanan ID: 123, Alasan: Product damaged, Status: PENDING_REVIEW";
        assertEquals(expectedData, refund.getData());
    }

    @Test
    void testRefundStatusEnum() {
        // Test all enum values exist
        assertNotNull(RefundStatus.PENDING_REVIEW);
        assertNotNull(RefundStatus.APPROVED);
        assertNotNull(RefundStatus.REJECTED);
        assertNotNull(RefundStatus.PROCESSING);
        assertNotNull(RefundStatus.COMPLETED);
        assertNotNull(RefundStatus.FAILED);
    }
}
