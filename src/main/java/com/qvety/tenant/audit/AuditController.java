package com.qvety.tenant.audit;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/audit", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AuditEntryDto> list(@RequestParam(required = false) String table,
                                    @RequestParam(required = false) UUID rowId,
                                    @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.list(table, rowId, pageable);
    }
}
