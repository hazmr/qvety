package com.qvety.scheduling;

import java.util.Map;
import java.util.Set;

/**
 * Mirrors the Postgres enum {@code appointment_status}. The transition table is data, not branches:
 * a status not listed as a key is terminal. Cancelling keeps the row and frees the slot.
 */
public enum AppointmentStatus {
    scheduled, checked_in, in_progress, completed, cancelled, no_show;

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED = Map.of(
        scheduled,   Set.of(checked_in, cancelled, no_show),
        checked_in,  Set.of(in_progress, cancelled),
        in_progress, Set.of(completed)
    );

    public boolean canMoveTo(AppointmentStatus next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }

    /** Cancelled and no-show rows are outside both exclusion constraints, so their slot is free again. */
    public boolean holdsItsSlot() {
        return this != cancelled && this != no_show;
    }
}
