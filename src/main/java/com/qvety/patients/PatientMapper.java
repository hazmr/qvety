package com.qvety.patients;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface PatientMapper {

    /** client.id on a LAZY proxy is the foreign key already loaded; no extra select. */
    @Mapping(target = "clientId", source = "client.id")
    PatientDto toDto(Patient patient);

    @Mapping(target = "patientId", source = "patient.id")
    PatientWeightDto toDto(PatientWeight weight);

    @Mapping(target = "patientId", source = "patient.id")
    PatientAllergyDto toDto(PatientAllergy allergy);

    /** Identity fields only: never id, practiceId, version, timestamps, client, previousClientId, deceasedAt, photo. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "species", source = "species")
    @Mapping(target = "breed", source = "breed")
    @Mapping(target = "sex", source = "sex", defaultValue = "unknown")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    @Mapping(target = "ageApproximate", source = "ageApproximate")
    @Mapping(target = "color", source = "color")
    @Mapping(target = "microchip", source = "microchip")
    @Mapping(target = "notes", source = "notes")
    void apply(PatientRequest request, @MappingTarget Patient patient);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "measuredAt", source = "measuredAt")
    @Mapping(target = "weightKg", source = "weightKg")
    void apply(WeightRequest request, @MappingTarget PatientWeight weight);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "substance", source = "substance")
    @Mapping(target = "reaction", source = "reaction")
    @Mapping(target = "severity", source = "severity")
    void apply(AllergyRequest request, @MappingTarget PatientAllergy allergy);
}
