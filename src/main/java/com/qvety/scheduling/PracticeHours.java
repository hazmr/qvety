package com.qvety.scheduling;

import com.qvety.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One opening range per weekday. Booking outside it warns and still saves. */
@Entity
@Table(name = "practice_hours")
@Getter
@Setter
@NoArgsConstructor
public class PracticeHours extends TenantEntity {

    /** 0 = Sunday, matching Postgres EXTRACT(DOW) and the Egyptian working week. */
    @Column(name = "weekday", nullable = false)
    private short weekday;

    @Column(name = "opens", nullable = false)
    private LocalTime opens;

    @Column(name = "closes", nullable = false)
    private LocalTime closes;
}
