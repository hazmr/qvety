package com.qvety.patients;

import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** List filters for GET /patients. Each returns null when the parameter is absent; the service drops nulls before composing. */
final class PatientSpecifications {

    private PatientSpecifications() {}

    static Specification<Patient> ofClient(UUID clientId) {
        return clientId == null ? null : (root, query, cb) -> cb.equal(root.get("client").get("id"), clientId);
    }

    static Specification<Patient> ofSpecies(Species species) {
        return species == null ? null : (root, query, cb) -> cb.equal(root.get("species"), species);
    }

    /** null: both; true: deceased only; false: living only. */
    static Specification<Patient> deceased(Boolean deceased) {
        if (deceased == null) {
            return null;
        }
        return (root, query, cb) -> deceased ? cb.isNotNull(root.get("deceasedAt")) : cb.isNull(root.get("deceasedAt"));
    }

    /** Case-insensitive substring on the name. No folded column here; patient names are short and per client. */
    static Specification<Patient> nameContains(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        var needle = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), needle);
    }
}
