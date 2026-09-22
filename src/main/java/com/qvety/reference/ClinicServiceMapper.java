package com.qvety.reference;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ClinicServiceMapper {

    ServiceDto toDto(ClinicService service);

    /** Request fields only: never id, practiceId, version, timestamps, active, currency. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "price", source = "price")
    void apply(ServiceRequest request, @MappingTarget ClinicService service);
}
