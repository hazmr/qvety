package com.qvety.tenant;

/** A transaction started with no authenticated user and no explicit system context. */
public class NoTenantException extends IllegalStateException {
    public NoTenantException() {
        super("transaction started without a tenant: no authenticated user and no SystemContext");
    }
}
