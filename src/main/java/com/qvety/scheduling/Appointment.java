package com.qvety.scheduling;

import com.qvety.clients.Client;
import com.qvety.patients.Patient;
import com.qvety.reference.AppointmentType;
import com.qvety.reference.Room;
import com.qvety.tenant.TenantEntity;
import com.qvety.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/**
 * One booking. The database column {@code period} is generated from startsAt and endsAt and is not mapped
 * here; it exists only for the two exclusion constraints that make overlap impossible. Every reference is
 * LAZY: the day grid and the board read what they need through native queries instead.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment extends TenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veterinarian_id", nullable = false)
    private User veterinarian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_type_id", nullable = false)
    private AppointmentType appointmentType;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "appointment_status")
    private AppointmentStatus status = AppointmentStatus.scheduled;

    /** Never updatable: a walk-in stays a walk-in for the daily report. */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "origin", nullable = false, updatable = false, columnDefinition = "appointment_origin")
    private AppointmentOrigin origin = AppointmentOrigin.scheduled;

    /** Set when the status becomes checked_in; the board reads minutes waited from it. */
    @Column(name = "checked_in_at")
    private OffsetDateTime checkedInAt;

    @Column(name = "reason")
    private String reason;

    @Column(name = "notes")
    private String notes;
}
