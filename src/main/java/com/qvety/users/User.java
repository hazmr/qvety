package com.qvety.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/** Staff account. Tenant table; part 06 moves the shared columns into {@code TenantEntity}. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // Postgres default uuidv7(), read back via RETURNING
    @Column(name = "id", insertable = false, updatable = false)
    private UUID id;

    @Column(name = "practice_id", nullable = false, updatable = false)
    private UUID practiceId;

    /** E.164. Required; a login identifier together with email. */
    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "role", nullable = false, columnDefinition = "user_role")
    private UserRole role;

    @Column(name = "is_veterinarian", nullable = false)
    private boolean veterinarian;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "session_version", nullable = false)
    private int sessionVersion = 1;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    /** Invalidates every token this user holds. */
    public void revokeSessions() {
        this.sessionVersion++;
    }
}
