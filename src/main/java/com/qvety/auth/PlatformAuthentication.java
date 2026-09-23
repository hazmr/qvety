package com.qvety.auth;

import java.util.Optional;
import java.util.UUID;

/**
 * How {@link JwtFilter} resolves a platform token without importing the platform package: auth owns the
 * interface, platform implements it. Feature code must never reach platform, and the filter is the one
 * place that has to understand both kinds of token.
 */
public interface PlatformAuthentication {

    /** Empty when the staff member is gone, deactivated, or the token's session version is stale. */
    Optional<PlatformPrincipal> verify(UUID userId, int sessionVersion);
}
