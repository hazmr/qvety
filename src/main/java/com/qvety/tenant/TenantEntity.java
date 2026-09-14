package com.qvety.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Shared columns of every tenant table. The id comes from Postgres (uuidv7()); practice_id is filled from
 * the current tenant on insert and never updated; version gives optimistic locking (409 on a stale save).
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // Postgres default uuidv7(), read back via RETURNING
    @Column(name = "id", insertable = false, updatable = false)
    private UUID id;

    @Column(name = "practice_id", nullable = false, updatable = false)
    private UUID practiceId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** A row can only be created inside a tenant; RLS WITH CHECK would refuse anything else anyway. */
    @PrePersist
    void assignPractice() {
        if (practiceId == null) {
            practiceId = TenantContext.current()
                .orElseThrow(NoTenantException::new)
                .practiceId();
        }
    }
}
