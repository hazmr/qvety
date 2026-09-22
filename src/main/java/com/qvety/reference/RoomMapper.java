package com.qvety.reference;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface RoomMapper {

    RoomDto toDto(Room room);

    /** Request fields only: never id, practiceId, version, timestamps, active. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    void apply(RoomRequest request, @MappingTarget Room room);
}
