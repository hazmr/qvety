package com.qvety.patients;

import com.qvety.tenant.NoTenantException;
import com.qvety.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/**
 * Never edited or deleted: a wrong entry is retracted with a reason and stays visible. Every column except
 * the retracted_* pair is updatable=false here, and the database trigger refuses any other change anyway.
 * No version column: the trigger would reject the bump.
 */
@Entity
@Table(name = "patient_allergies")
@Getter
@Setter
@NoArgsConstructor
public class PatientAllergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, updatable = false)
    private UUID id;

    @Column(name = "practice_id", nullable = false, updatable = false)
    private UUID practiceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @Column(name = "substance", nullable = false, updatable = false)
    private String substance;

    @Column(name = "reaction", updatable = false)
    private String reaction;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "severity", nullable = false, updatable = false, columnDefinition = "allergy_severity")
    private AllergySeverity severity;

    @Column(name = "noted_by", nullable = false, updatable = false)
    private UUID notedBy;

    @Column(name = "retracted_at")
    private OffsetDateTime retractedAt;

    @Column(name = "retracted_reason")
    private String retractedReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public boolean isRetracted() {
        return retractedAt != null;
    }

    @PrePersist
    void assignPractice() {
        if (practiceId == null) {
            practiceId = TenantContext.current().orElseThrow(NoTenantException::new).practiceId();
        }
    }
}
