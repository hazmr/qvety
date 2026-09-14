package com.qvety.clients;

import com.qvety.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The pet owner. Archived, never deleted. Phones are stored as typed until part 07 adds the E.164 columns. */
@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client extends TenantEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "preferred_name")
    private String preferredName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "phone_secondary")
    private String phoneSecondary;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "notes")
    private String notes;

    @Column(name = "preferred_locale")
    private String preferredLocale;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }
}
