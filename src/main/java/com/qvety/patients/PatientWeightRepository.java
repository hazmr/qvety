package com.qvety.patients;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientWeightRepository extends JpaRepository<PatientWeight, UUID> {

    List<PatientWeight> findByPatientIdOrderByMeasuredAtDesc(UUID patientId);

    /** The header weight: latest measurement still standing. */
    Optional<PatientWeight> findFirstByPatientIdAndVoidedAtIsNullOrderByMeasuredAtDesc(UUID patientId);
}
