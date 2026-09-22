package com.qvety.scheduling;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Read side only. The inbound request names five rows by id, so the service loads them and sets them
 * itself; there is nothing MapStruct could map onto an entity without guessing.
 */
@Mapper
public interface AppointmentMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.name")
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.fullName")
    @Mapping(target = "veterinarianId", source = "veterinarian.id")
    @Mapping(target = "veterinarianName", source = "veterinarian.fullName")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomName", source = "room.name")
    @Mapping(target = "appointmentTypeId", source = "appointmentType.id")
    @Mapping(target = "appointmentTypeName", source = "appointmentType.name")
    @Mapping(target = "color", source = "appointmentType.color")
    AppointmentDto toDto(Appointment appointment);
}
