package com.qvety.scheduling;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The two read models over the same table: the day grid the desk books in, and the whiteboard. */
@RestController
@RequestMapping(value = "/api/v1/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "schedule")
public class ScheduleController {

    private final AppointmentService service;

    public ScheduleController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping("/day")
    public DayDto getDay(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam(required = false) UUID veterinarianId) {
        return service.day(date, veterinarianId);
    }

    @GetMapping("/board")
    public BoardDto getBoard() {
        return service.board();
    }
}
