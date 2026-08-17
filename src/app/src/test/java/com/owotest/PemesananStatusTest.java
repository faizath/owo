package com.owotest;

import com.owo.entity.PemesananStatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PemesananStatusTest {

    @Test
    void fromDb_toleratesCase() {
        // Comparison used to be case-sensitive in one controller and insensitive in two
        // others, so "confirmed" passed check-in but failed confirmation.
        assertEquals(PemesananStatus.CONFIRMED, PemesananStatus.fromDb("confirmed"));
        assertEquals(PemesananStatus.CONFIRMED, PemesananStatus.fromDb("  CONFIRMED "));
    }

    @Test
    void fromDb_rejectsUnknownAndMissingValues() {
        assertThrows(IllegalArgumentException.class, () -> PemesananStatus.fromDb("SOMETHING"));
        assertThrows(IllegalArgumentException.class, () -> PemesananStatus.fromDb(null));
        assertThrows(IllegalArgumentException.class, () -> PemesananStatus.fromDb(""));
    }

    @Test
    void paymentAndCheckInFollowTheHappyPath() {
        assertTrue(PemesananStatus.PENDING.canTransitionTo(PemesananStatus.CONFIRMED));
        assertTrue(PemesananStatus.CONFIRMED.canTransitionTo(PemesananStatus.CHECKED_IN));
    }

    @Test
    void anUnpaidBookingCannotBeCheckedInto() {
        assertFalse(PemesananStatus.PENDING.canTransitionTo(PemesananStatus.CHECKED_IN));
    }

    @Test
    void terminalStatesAdmitNoFurtherChange() {
        assertTrue(PemesananStatus.CANCELLED.isTerminal());
        assertTrue(PemesananStatus.REFUNDED.isTerminal());

        // Cancelling a refunded booking used to be permitted, orphaning its refund record.
        assertFalse(PemesananStatus.REFUNDED.canTransitionTo(PemesananStatus.CANCELLED));
        assertFalse(PemesananStatus.CANCELLED.canTransitionTo(PemesananStatus.CONFIRMED));
    }

    @Test
    void aRejectedRefundCanRestoreEitherPriorState() {
        assertTrue(PemesananStatus.REFUND_IN_PROGRESS.canTransitionTo(PemesananStatus.CONFIRMED));
        assertTrue(PemesananStatus.REFUND_IN_PROGRESS.canTransitionTo(PemesananStatus.CHECKED_IN));
    }

    @Test
    void aCheckedInBookingCannotBeCancelled() {
        assertFalse(PemesananStatus.CHECKED_IN.canTransitionTo(PemesananStatus.CANCELLED));
    }
}
