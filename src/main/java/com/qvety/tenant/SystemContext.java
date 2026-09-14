package com.qvety.tenant;

import java.util.concurrent.Callable;

/**
 * Explicit opt-out from the tenant requirement. Inside {@code run} the transaction hook sets no
 * practice, so row-level security shows nothing except what definer functions expose
 * (login_lookup) and, from part 11, platform tables. Never ambient; part 11 restricts callers.
 */
public final class SystemContext {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private SystemContext() {}

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static <T> T call(Callable<T> body) {
        boolean previous = ACTIVE.get();
        ACTIVE.set(true);
        try {
            return body.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            ACTIVE.set(previous);
        }
    }

    public static void run(Runnable body) {
        call(() -> { body.run(); return null; });
    }
}
