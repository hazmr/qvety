package com.qvety.users;

import com.qvety.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/** Staff account. Tenant table on the shared base. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends TenantEntity {

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

    /** ar-EG or en-EG; drives UI language and direction. */
    @Column(name = "locale", nullable = false)
    private String locale = "ar-EG";

    /** Invalidates every token this user holds. */
    public void revokeSessions() {
        this.sessionVersion++;
    }
}
