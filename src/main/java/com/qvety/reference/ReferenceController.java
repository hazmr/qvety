package com.qvety.reference;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The six endpoints every reference list has. A subclass adds only {@code @RestController},
 * {@code @RequestMapping}, and its constructor: Spring resolves R and D from the concrete class, so
 * springdoc still emits a typed request and response schema per entity instead of one shared Object.
 *
 * The list is not paged. A clinic has a handful of rooms and a few dozen services, the same reason
 * {@code /api/v1/users} returns a plain list.
 */
public abstract class ReferenceController<R, D> {

    private final ReferenceService<?, R, D> service;

    protected ReferenceController(ReferenceService<?, R, D> service) {
        this.service = service;
    }

    @GetMapping
    public List<D> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.list(includeInactive);
    }

    @GetMapping("/{id}")
    public D get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public D create(@Valid @RequestBody R request) {
        return service.create(request);
    }

    /** If-Match carries the version the client last saw; a stale one answers 409 stale_update. */
    @PutMapping("/{id}")
    public D update(@PathVariable UUID id, @Valid @RequestBody R request, @RequestHeader("If-Match") long version) {
        return service.update(id, request, version);
    }

    @PostMapping("/{id}/deactivate")
    public D deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public D activate(@PathVariable UUID id) {
        return service.activate(id);
    }
}
