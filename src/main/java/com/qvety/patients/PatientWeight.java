package com.qvety.patients;

import com.qvety.tenant.NoTenantException;
import com.qvety.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** One dated weight in kilograms. Insert, then at most one void; no version column and no TenantEntity. */
@Entity
@Table(name = "patient_weights")
@Getter
@Setter
@NoArgsConstructor
public class PatientWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, updatable = false)
    private UUID id;

    @Column(name = "practice_id", nullable = false, updatable = false)
    private UUID practiceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @Column(name = "measured_at", nullable = false, updatable = false)
    private OffsetDateTime measuredAt;

    @Column(name = "weight_kg", nullable = false, updatable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "voided_at")
    private OffsetDateTime voidedAt;

    @Column(name = "void_reason")
    private String voidReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public boolean isVoided() {
        return voidedAt != null;
    }

    @PrePersist
    void assignPractice() {
        if (practiceId == null) {
            practiceId = TenantContext.current().orElseThrow(NoTenantException::new).practiceId();
        }
    }
}
