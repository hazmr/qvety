package com.qvety.platform;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAuditRepository extends JpaRepository<PlatformAuditEntry, UUID> {
}
