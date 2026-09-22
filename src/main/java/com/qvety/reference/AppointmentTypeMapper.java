package com.qvety.reference;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface AppointmentTypeMapper {

    AppointmentTypeDto toDto(AppointmentType type);

    /** Request fields only: never id, practiceId, version, timestamps, active. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "durationMinutes", source = "durationMinutes")
    @Mapping(target = "color", source = "color")
    void apply(AppointmentTypeRequest request, @MappingTarget AppointmentType type);
}
