package com.qvety.auth;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** The one place services read the caller from. Every later service and the tenant hook (part 04) use it. */
@Component
public class CurrentUser {

    public AuthenticatedUser get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("no authenticated user in this context");
        }
        return user;
    }

    public UUID userId() {
        return get().userId();
    }

    public UUID practiceId() {
        return get().practiceId();
    }

    public boolean isVeterinarian() {
        return get().veterinarian();
    }
}
