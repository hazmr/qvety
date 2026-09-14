package com.qvety.tenant.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditRepository repository;
    private final AuditMapper mapper;

    public AuditService(AuditRepository repository, AuditMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Admin only. Ordering is fixed (newest first); the caller picks the row or table. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<AuditEntryDto> list(String table, UUID rowId, Pageable pageable) {
        Page<AuditEntry> page;
        if (table != null && rowId != null) {
            page = repository.findByTableNameAndRowIdOrderByAtDesc(table, rowId, pageable);
        } else if (table != null) {
            page = repository.findByTableNameOrderByAtDesc(table, pageable);
        } else {
            page = repository.findAllByOrderByAtDesc(pageable);
        }
        return page.map(mapper::toDto);
    }
}
