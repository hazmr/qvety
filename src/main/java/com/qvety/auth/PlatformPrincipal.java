package com.qvety.auth;

import java.util.UUID;

/**
 * The principal for a Qvety staff member. No practice: nothing here may be used to enter a tenant.
 * Defined in auth because {@link JwtFilter} places it, and filled by the platform package.
 */
public record PlatformPrincipal(UUID userId, String fullName) {

    /** The authority platform endpoints require. */
    public static final String AUTHORITY = "PLATFORM";
}
