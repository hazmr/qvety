package com.qvety.reference;

import com.qvety.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What every reference table has: a name, unique per practice, and an active flag. Deactivated rows stay,
 * because appointments (part 10) and invoice lines (part 14) point at them.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class ReferenceEntity extends TenantEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
