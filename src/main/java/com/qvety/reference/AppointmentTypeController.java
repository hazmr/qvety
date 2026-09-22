package com.qvety.reference;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/reference/appointment-types", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "reference")
public class AppointmentTypeController extends ReferenceController<AppointmentTypeRequest, AppointmentTypeDto> {

    public AppointmentTypeController(AppointmentTypeService service) {
        super(service);
    }
}
