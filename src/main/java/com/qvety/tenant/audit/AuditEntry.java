package com.qvety.tenant.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

/** Read-only view of audit_log. Rows are written by the audit_row() trigger only; the app never inserts or changes them. */
@Entity
@Immutable
@Table(name = "audit_log")
@Getter
@NoArgsConstructor
public class AuditEntry {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "practice_id", nullable = false)
    private UUID practiceId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "row_id", nullable = false)
    private UUID rowId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "action", nullable = false, columnDefinition = "audit_action")
    private AuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before")
    private Map<String, Object> before;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after")
    private Map<String, Object> after;

    @Column(name = "at", nullable = false)
    private OffsetDateTime at;
}
