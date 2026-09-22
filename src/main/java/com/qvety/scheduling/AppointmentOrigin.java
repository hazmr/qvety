package com.qvety.scheduling;

/** Mirrors the Postgres enum {@code appointment_origin}. Set once at creation; the daily report counts one against the other. */
public enum AppointmentOrigin {
    scheduled, walk_in
}
