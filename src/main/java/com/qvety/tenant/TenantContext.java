package com.qvety.tenant;

import com.qvety.auth.AuthenticatedUser;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the tenant for the current thread: an explicit scope (used by the JWT filter before the
 * principal exists) wins, then the authenticated user. Nothing else.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantScope> EXPLICIT = new ThreadLocal<>();

    private TenantContext() {}

    public static Optional<TenantScope> current() {
        var explicit = EXPLICIT.get();
        if (explicit != null) {
            return Optional.of(explicit);
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(new TenantScope(user.practiceId(), user.userId()));
        }
        return Optional.empty();
    }

    /** Run with a fixed tenant, e.g. the one named in a verified token before the user row is loaded. */
    public static <T> T runAs(TenantScope scope, Supplier<T> body) {
        var previous = EXPLICIT.get();
        EXPLICIT.set(scope);
        try {
            return body.get();
        } finally {
            if (previous == null) EXPLICIT.remove(); else EXPLICIT.set(previous);
        }
    }
}
