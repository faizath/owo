package com.owo.entity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The booking lifecycle, and the only transitions it permits.
 *
 * <pre>
 * PENDING ──pay──▶ CONFIRMED ──check-in──▶ CHECKED_IN
 *    │                  │                       │
 *    └──cancel──▶ CANCELLED                     │
 *                       │                       │
 *                       └──refund──▶ REFUND_IN_PROGRESS ──▶ REFUNDED
 *                                          │
 *                                          └──reject──▶ (previous status)
 * </pre>
 *
 * <p>Replaces seven bare string literals that were compared case-sensitively in one
 * controller and case-insensitively in two others, so a status written as
 * {@code "confirmed"} passed check-in and refund but failed confirmation.
 */
public enum PemesananStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CANCELLED,
    REFUND_IN_PROGRESS,
    REFUNDED;

    private static final Map<PemesananStatus, Set<PemesananStatus>> TRANSITIONS;

    static {
        Map<PemesananStatus, Set<PemesananStatus>> t = new EnumMap<>(PemesananStatus.class);
        t.put(PENDING, EnumSet.of(CONFIRMED, CANCELLED));
        t.put(CONFIRMED, EnumSet.of(CHECKED_IN, CANCELLED, REFUND_IN_PROGRESS));
        t.put(CHECKED_IN, EnumSet.of(REFUND_IN_PROGRESS));
        // A refund may be approved, or rejected back to whatever the booking was before.
        t.put(REFUND_IN_PROGRESS, EnumSet.of(REFUNDED, CONFIRMED, CHECKED_IN));
        t.put(CANCELLED, EnumSet.noneOf(PemesananStatus.class));
        t.put(REFUNDED, EnumSet.noneOf(PemesananStatus.class));
        TRANSITIONS = Collections.unmodifiableMap(t);
    }

    /** The value written to and read from the {@code status} column. */
    public String dbValue() {
        return name();
    }

    /**
     * Parses a stored status, tolerating case but not unknown values.
     *
     * @throws IllegalArgumentException if the value is absent or unrecognised, rather than
     *     letting a bad row surface later as a null status or a silent mismatch
     */
    public static PemesananStatus fromDb(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Booking status is missing");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unrecognised booking status: " + value, e);
        }
    }

    public boolean canTransitionTo(PemesananStatus target) {
        return target != null && TRANSITIONS.get(this).contains(target);
    }

    public Set<PemesananStatus> allowedTransitions() {
        return TRANSITIONS.get(this);
    }

    /** True once the booking can no longer change. */
    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
