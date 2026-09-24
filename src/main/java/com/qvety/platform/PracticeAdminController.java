package com.qvety.platform;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Super admin only; the authority is checked on the service, so a missing annotation here cannot open it. */
@RestController
@RequestMapping(value = "/api/platform/practices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "platform")
public class PracticeAdminController {

    private final PracticeAdminService service;

    public PracticeAdminController(PracticeAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<PracticeSummaryDto> listPractices() {
        return service.list();
    }

    @GetMapping("/{id}")
    public PracticeSummaryDto getPlatformPractice(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PracticeCreatedDto createPractice(@Valid @RequestBody CreatePracticeRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/status")
    public PracticeSummaryDto changePracticeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(id, request.status(), request.reason());
    }
}
