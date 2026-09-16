package com.qvety.patients;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/patients", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    public Page<PatientDto> listPatients(@RequestParam(required = false) UUID clientId,
                                         @RequestParam(required = false) Species species,
                                         @RequestParam(required = false) Boolean deceased,
                                         @RequestParam(required = false) String q,
                                         @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.list(clientId, species, deceased, q, pageable);
    }

    @GetMapping("/{id}")
    public PatientDetailDto getPatient(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDto createPatient(@Valid @RequestBody PatientRequest request) {
        return service.create(request);
    }

    /** If-Match carries the version the client last saw; a stale one answers 409 stale_update. */
    @PutMapping("/{id}")
    public PatientDto updatePatient(@PathVariable UUID id, @Valid @RequestBody PatientRequest request,
                                    @RequestHeader("If-Match") long version) {
        return service.update(id, request, version);
    }

    @PostMapping("/{id}/deceased")
    public PatientDto markDeceased(@PathVariable UUID id, @Valid @RequestBody DeceasedRequest request) {
        return service.markDeceased(id, request);
    }

    @PostMapping("/{id}/transfer")
    public PatientDto transferPatient(@PathVariable UUID id, @Valid @RequestBody TransferRequest request) {
        return service.transfer(id, request);
    }

    @GetMapping("/{id}/weights")
    public List<PatientWeightDto> listWeights(@PathVariable UUID id) {
        return service.listWeights(id);
    }

    @PostMapping("/{id}/weights")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientWeightDto addWeight(@PathVariable UUID id, @Valid @RequestBody WeightRequest request) {
        return service.addWeight(id, request);
    }

    @PostMapping("/{id}/weights/{weightId}/void")
    public PatientWeightDto voidWeight(@PathVariable UUID id, @PathVariable UUID weightId,
                                       @Valid @RequestBody RetractRequest request) {
        return service.voidWeight(id, weightId, request);
    }

    @GetMapping("/{id}/allergies")
    public List<PatientAllergyDto> listAllergies(@PathVariable UUID id) {
        return service.listAllergies(id);
    }

    @PostMapping("/{id}/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientAllergyDto addAllergy(@PathVariable UUID id, @Valid @RequestBody AllergyRequest request) {
        return service.addAllergy(id, request);
    }

    @PostMapping("/{id}/allergies/{allergyId}/retract")
    public PatientAllergyDto retractAllergy(@PathVariable UUID id, @PathVariable UUID allergyId,
                                            @Valid @RequestBody RetractRequest request) {
        return service.retractAllergy(id, allergyId, request);
    }
}
