package com.qvety.auth;

import org.springframework.stereotype.Component;

/** Used in SpEL: {@code @PreAuthorize("@access.isVeterinarian()")}. Clinical acts check the flag, never the role. */
@Component("access")
public class AccessChecks {

    private final CurrentUser currentUser;

    public AccessChecks(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isVeterinarian() {
        return currentUser.isVeterinarian();
    }
}
