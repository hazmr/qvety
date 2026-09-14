package com.qvety.practice;

/** Mirrors the Postgres enum {@code practice_status}. Changed only by the super admin or the subscription job. */
public enum PracticeStatus {
    trial, active, past_due, suspended, closed
}
