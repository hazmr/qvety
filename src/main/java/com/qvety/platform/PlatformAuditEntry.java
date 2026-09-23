package com.qvety.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Who did what to which practice. Append-only: the table's trigger refuses UPDATE and DELETE even to the
 * owner, so this is the record that answers "who suspended us, and why".
 */
@Entity
@Immutable
@Table(name = "platform_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class PlatformAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, updatable = false)
    private UUID id;

    /** Null only for a row written by a job rather than a person (part 12). */
    @Column(name = "platform_user_id")
    private UUID platformUserId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    /** Free-form context: the reason for a status change, the country of a new practice. Real jsonb. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @CreationTimestamp
    @Column(name = "at", nullable = false, updatable = false)
    private OffsetDateTime at;
}
