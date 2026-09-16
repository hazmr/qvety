package com.qvety.patients;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** RLS scopes every query to the caller's practice. List filters are Specifications ({@link PatientSpecifications}). */
public interface PatientRepository extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {
}
