package com.qvety.users;

/** Mirrors the Postgres enum {@code user_role}. Decides what a user may manage; never used for clinical acts. */
public enum UserRole {
    admin, veterinarian, technician, front_desk
}
