package com.qvety.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Qvety staff. Deliberately not a flag on {@code users}: that table is under forced row-level security
 * with a mandatory practice_id, and a super admin belongs to no practice. It does not extend
 * TenantEntity for the same reason, so it carries its own id and timestamps.
 */
@Entity
@Table(name = "platform_users")
@Getter
@Setter
@NoArgsConstructor
public class PlatformUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // Postgres default uuidv7(), read back via RETURNING
    @Column(name = "id", insertable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Bumped to revoke every token of this staff member, the same rule as users.session_version. */
    @Column(name = "session_version", nullable = false)
    private int sessionVersion = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
