package com.qvety.reference;

import org.springframework.stereotype.Service;

/** Appointment types. Every rule lives in {@link ReferenceService}. */
@Service
public class AppointmentTypeService extends ReferenceService<AppointmentType, AppointmentTypeRequest, AppointmentTypeDto> {

    private final AppointmentTypeMapper mapper;

    public AppointmentTypeService(AppointmentTypeRepository repository, AppointmentTypeMapper mapper) {
        super(repository);
        this.mapper = mapper;
    }

    @Override
    protected AppointmentType newEntity() {
        return new AppointmentType();
    }

    @Override
    protected void apply(AppointmentTypeRequest request, AppointmentType type) {
        mapper.apply(request, type);
        if (type.getColor() != null && type.getColor().isBlank()) {
            type.setColor(null);
        }
    }

    @Override
    protected AppointmentTypeDto toDto(AppointmentType type) {
        return mapper.toDto(type);
    }

    @Override
    protected String name(AppointmentTypeRequest request) {
        return request.name();
    }
}
