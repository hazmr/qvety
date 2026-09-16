package com.qvety.patients;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, UUID> {

    /** All rows, retracted included: a retracted allergy stays visible as retracted. */
    List<PatientAllergy> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<PatientAllergy> findByPatientIdAndRetractedAtIsNullOrderByCreatedAtDesc(UUID patientId);
}
