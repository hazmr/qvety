package com.qvety.practice;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface PracticeMapper {

    PracticeDto toDto(Practice practice);

    /** Identity columns only; everything else on the entity is ignored on purpose. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "vatNumber", source = "vatNumber")
    @Mapping(target = "taxRatePercent", source = "taxRatePercent")
    void updateFromRequest(PracticeUpdateRequest request, @MappingTarget Practice practice);
}
