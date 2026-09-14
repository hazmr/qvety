package com.qvety.tenant;

import java.util.UUID;

/** What the transaction hook writes into the Postgres session: the practice and, when known, the user. */
public record TenantScope(UUID practiceId, UUID userId) {}
