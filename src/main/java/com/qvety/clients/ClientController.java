package com.qvety.clients;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping(value = "/api/v1/clients", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "clients")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ClientDto> listClients(@RequestParam(required = false) String q,
                                @RequestParam(defaultValue = "false") boolean includeArchived,
                                @ParameterObject @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.list(q, includeArchived, pageable);
    }

    @GetMapping("/{id}")
    public ClientDto getClient(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientSavedDto createClient(@Valid @RequestBody ClientRequest request) {
        return service.create(request);
    }

    /** If-Match carries the version the client last saw; a stale one answers 409 stale_update. */
    @PutMapping("/{id}")
    public ClientSavedDto updateClient(@PathVariable UUID id, @Valid @RequestBody ClientRequest request,
                                 @RequestHeader("If-Match") long version) {
        return service.update(id, request, version);
    }

    @PostMapping("/{id}/archive")
    public ClientDto archiveClient(@PathVariable UUID id) {
        return service.archive(id);
    }

    @PostMapping("/{id}/unarchive")
    public ClientDto unarchiveClient(@PathVariable UUID id) {
        return service.unarchive(id);
    }
}
