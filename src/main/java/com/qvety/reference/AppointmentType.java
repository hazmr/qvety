package com.qvety.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A kind of visit with its default length and day-view colour. Part 14 adds the default service. */
@Entity
@Table(name = "appointment_types")
@Getter
@Setter
@NoArgsConstructor
public class AppointmentType extends ReferenceEntity {

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "color")
    private String color;
}
