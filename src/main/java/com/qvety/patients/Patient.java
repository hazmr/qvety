package com.qvety.patients;

import com.qvety.clients.Client;
import com.qvety.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/**
 * The animal. Belongs to one client of the same practice (composite FK in the database); transferred or
 * marked deceased, never deleted. The client is LAZY: a patient read must not drag the owner along.
 */
@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
public class Patient extends TenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Set on transfer; the id is enough, nothing reads the previous owner's row from here. */
    @Column(name = "previous_client_id")
    private UUID previousClientId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "species", nullable = false, columnDefinition = "species")
    private Species species;

    @Column(name = "breed")
    private String breed;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "sex", nullable = false, columnDefinition = "patient_sex")
    private PatientSex sex = PatientSex.unknown;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "age_approximate")
    private String ageApproximate;

    @Column(name = "color")
    private String color;

    @Column(name = "microchip")
    private String microchip;

    /** Written from part 13; read-only until then. */
    @Column(name = "photo_object_key", updatable = false)
    private String photoObjectKey;

    @Column(name = "deceased_at")
    private LocalDate deceasedAt;

    @Column(name = "notes")
    private String notes;

    public boolean isDeceased() {
        return deceasedAt != null;
    }
}
