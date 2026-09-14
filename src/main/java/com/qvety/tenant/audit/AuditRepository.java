package com.qvety.tenant.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** RLS scopes every query to the caller's practice; no practice_id parameter needed. */
public interface AuditRepository extends JpaRepository<AuditEntry, UUID> {

    Page<AuditEntry> findByTableNameAndRowIdOrderByAtDesc(String tableName, UUID rowId, Pageable pageable);

    Page<AuditEntry> findByTableNameOrderByAtDesc(String tableName, Pageable pageable);

    Page<AuditEntry> findAllByOrderByAtDesc(Pageable pageable);
}
