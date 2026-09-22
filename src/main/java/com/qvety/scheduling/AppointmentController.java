package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping(value = "/api/v1/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    /** One day at a time; without a date, today in the practice timezone. */
    @GetMapping
    public List<AppointmentDto> listAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID veterinarianId) {
        return service.list(date, veterinarianId);
    }

    @GetMapping("/{id}")
    public AppointmentDto getAppointment(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentSavedDto createAppointment(@Valid @RequestBody AppointmentRequest request) {
        return service.create(request);
    }

    /** If-Match carries the version the client last saw; a stale one answers 409 stale_update. */
    @PutMapping("/{id}")
    public AppointmentSavedDto updateAppointment(@PathVariable UUID id, @Valid @RequestBody AppointmentRequest request,
                                                 @RequestHeader("If-Match") long version) {
        return service.update(id, request, version);
    }

    @PostMapping("/{id}/status")
    public AppointmentDto changeAppointmentStatus(@PathVariable UUID id, @Valid @RequestBody StatusChangeRequest request) {
        return service.changeStatus(id, request.status());
    }
}
