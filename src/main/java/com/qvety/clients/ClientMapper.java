package com.qvety.clients;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ClientMapper {

    ClientDto toDto(Client client);

    /** Request fields only: never id, practiceId, version, timestamps, archivedAt. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "preferredName", source = "preferredName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "phoneSecondary", source = "phoneSecondary")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "preferredLocale", source = "preferredLocale")
    void apply(ClientRequest request, @MappingTarget Client client);
}
