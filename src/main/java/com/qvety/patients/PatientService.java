package com.qvety.patients;

import com.qvety.clients.Client;
import com.qvety.clients.ClientRepository;
import com.qvety.common.DomainException;
import com.qvety.tenant.NoTenantException;
import com.qvety.tenant.TenantContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Identity, weights, deceased and transfer are open to every role: front desk manages patients, clinical
 * staff correct them at the table. Allergies are clinical data, so front desk never writes or retracts them.
 * Nothing here deletes. Cross-tenant ids are invisible under RLS and answer 404.
 */
@Service
public class PatientService {

    private final PatientRepository patients;
    private final PatientWeightRepository weights;
    private final PatientAllergyRepository allergies;
    private final ClientRepository clients;
    private final PatientMapper mapper;

    public PatientService(PatientRepository patients, PatientWeightRepository weights,
                          PatientAllergyRepository allergies, ClientRepository clients, PatientMapper mapper) {
        this.patients = patients;
        this.weights = weights;
        this.allergies = allergies;
        this.clients = clients;
        this.mapper = mapper;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<PatientDto> list(UUID clientId, Species species, Boolean deceased, String q, Pageable pageable) {
        // absent filters are null; Specification.and rejects null, so they are dropped before composing
        var spec = Specification.allOf(Stream.of(
            PatientSpecifications.ofClient(clientId),
            PatientSpecifications.ofSpecies(species),
            PatientSpecifications.deceased(deceased),
            PatientSpecifications.nameContains(q)).filter(Objects::nonNull).toList());
        return patients.findAll(spec, pageable).map(mapper::toDto);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PatientDetailDto get(UUID id) {
        var patient = load(id);
        var latest = weights.findFirstByPatientIdAndVoidedAtIsNullOrderByMeasuredAtDesc(id).map(mapper::toDto).orElse(null);
        var active = allergies.findByPatientIdAndRetractedAtIsNullOrderByCreatedAtDesc(id).stream().map(mapper::toDto).toList();
        return new PatientDetailDto(mapper.toDto(patient), latest, active);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PatientDto create(PatientRequest request) {
        var patient = new Patient();
        patient.setClient(loadClient(request.clientId()));
        mapper.apply(request, patient);
        return mapper.toDto(patients.saveAndFlush(patient));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PatientDto update(UUID id, PatientRequest request, long version) {
        var patient = load(id);
        if (!patient.getClient().getId().equals(request.clientId())) {   // owner changes go through transfer
            throw DomainException.badRequest("patient.owner_change_is_transfer", "clientId");
        }
        if (patient.getVersion() != version) {   // the caller edited an older copy (two tabs, two people)
            throw DomainException.conflict("stale_update");
        }
        mapper.apply(request, patient);
        return mapper.toDto(patients.saveAndFlush(patient));
    }

    /** A date, never a delete: the patient leaves recalls and default lists but every record stays. */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PatientDto markDeceased(UUID id, DeceasedRequest request) {
        var patient = load(id);
        patient.setDeceasedAt(request.date());
        return mapper.toDto(patients.saveAndFlush(patient));
    }

    /**
     * Written by hand, not a plain field update: the target must be visible in this practice (RLS makes any
     * other practice's client a 404) and the previous owner is kept. Weights, allergies, and later visits
     * hang off the patient, so they follow it untouched. A deceased patient stays with the owner who lost it.
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PatientDto transfer(UUID id, TransferRequest request) {
        var patient = load(id);
        if (patient.isDeceased()) {
            throw DomainException.conflict("patient.transfer_deceased");
        }
        var target = loadClient(request.clientId());
        if (target.getId().equals(patient.getClient().getId())) {
            throw DomainException.conflict("patient.transfer_same_client");
        }
        patient.setPreviousClientId(patient.getClient().getId());
        patient.setClient(target);
        return mapper.toDto(patients.saveAndFlush(patient));
    }

    // ---- weights ------------------------------------------------------------------------------------

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<PatientWeightDto> listWeights(UUID patientId) {
        load(patientId);
        return weights.findByPatientIdOrderByMeasuredAtDesc(patientId).stream().map(mapper::toDto).toList();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PatientWeightDto addWeight(UUID patientId, WeightRequest request) {
        var weight = new PatientWeight();
        weight.setPatient(load(patientId));
        mapper.apply(request, weight);
        return mapper.toDto(weights.saveAndFlush(weight));
    }

    /** A measurement is never corrected in place; the only change the row ever takes, the trigger refuses the rest. */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PatientWeightDto voidWeight(UUID patientId, UUID weightId, RetractRequest request) {
        var weight = weights.findById(weightId)
            .filter(w -> w.getPatient().getId().equals(patientId))
            .orElseThrow(() -> DomainException.notFound("weight.not_found"));
        if (weight.isVoided()) {
            throw DomainException.conflict("weight.already_voided");
        }
        weight.setVoidedAt(OffsetDateTime.now());
        weight.setVoidReason(request.reason().trim());
        return mapper.toDto(weights.saveAndFlush(weight));
    }

    // ---- allergies ----------------------------------------------------------------------------------

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<PatientAllergyDto> listAllergies(UUID patientId) {
        load(patientId);
        return allergies.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(mapper::toDto).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIAN', 'TECHNICIAN')")
    @Transactional
    public PatientAllergyDto addAllergy(UUID patientId, AllergyRequest request) {
        var allergy = new PatientAllergy();
        allergy.setPatient(load(patientId));
        allergy.setNotedBy(currentUserId());
        mapper.apply(request, allergy);
        return mapper.toDto(allergies.saveAndFlush(allergy));
    }

    /** The only change the row ever takes; the database trigger refuses everything else. */
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIAN', 'TECHNICIAN')")
    @Transactional
    public PatientAllergyDto retractAllergy(UUID patientId, UUID allergyId, RetractRequest request) {
        var allergy = allergies.findById(allergyId)
            .filter(a -> a.getPatient().getId().equals(patientId))
            .orElseThrow(() -> DomainException.notFound("allergy.not_found"));
        if (allergy.isRetracted()) {
            throw DomainException.conflict("allergy.already_retracted");
        }
        allergy.setRetractedAt(OffsetDateTime.now());
        allergy.setRetractedReason(request.reason().trim());
        return mapper.toDto(allergies.saveAndFlush(allergy));
    }

    private Patient load(UUID id) {
        return patients.findById(id).orElseThrow(() -> DomainException.notFound("patient.not_found"));
    }

    private Client loadClient(UUID id) {
        return clients.findById(id).orElseThrow(() -> DomainException.notFound("client.not_found"));
    }

    private static UUID currentUserId() {
        return TenantContext.current().orElseThrow(NoTenantException::new).userId();
    }
}
