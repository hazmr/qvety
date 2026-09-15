package com.qvety.clients;

import com.qvety.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The pet owner. Archived, never deleted. full_name and the phones stay as typed; the folded and E.164
 * columns beside them are set by the service on every save and never shown.
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client extends TenantEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "full_name_normalized", nullable = false)
    private String fullNameNormalized;

    @Column(name = "preferred_name")
    private String preferredName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "phone_e164")
    private String phoneE164;

    @Column(name = "phone_secondary")
    private String phoneSecondary;

    @Column(name = "phone_secondary_e164")
    private String phoneSecondaryE164;

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
