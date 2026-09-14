package com.qvety.auth;

import com.qvety.users.UserRole;
import java.util.UUID;

/** The principal placed in the security context by {@link JwtFilter}. Loaded from the user row on every request. */
public record AuthenticatedUser(
    UUID userId,
    UUID practiceId,
    UserRole role,
    boolean veterinarian,
    String fullName,
    boolean mustChangePassword
) {}
