package com.qvety.reference;

import com.qvety.common.DomainException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one service behind rooms, appointment types, and services. Everyone reads; only an admin writes.
 * Inactive rows are hidden unless asked for; deactivate and activate are both idempotent, so pressing
 * either twice is not a fault. A row is never deleted (docs/domain/reference-data.md).
 *
 * A subclass supplies only the four entity-shaped operations; there is no business rule here that a
 * fourth reference table would not want.
 */
public abstract class ReferenceService<E extends ReferenceEntity, R, D> {

    protected final ReferenceRepository<E> repository;

    protected ReferenceService(ReferenceRepository<E> repository) {
        this.repository = repository;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<D> list(boolean includeInactive) {
        return repository.listOrdered(includeInactive).stream().map(this::toDto).toList();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public D get(UUID id) {
        return toDto(load(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public D create(R request) {
        var entity = newEntity();
        requireNameFree(name(request), null);
        apply(request, entity);
        entity.setName(entity.getName().trim());
        return toDto(repository.saveAndFlush(entity));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public D update(UUID id, R request, long version) {
        var entity = load(id);
        if (entity.getVersion() != version) {   // the caller edited an older copy (two tabs, two people)
            throw DomainException.conflict("stale_update");
        }
        requireNameFree(name(request), id);
        apply(request, entity);
        entity.setName(entity.getName().trim());
        return toDto(repository.saveAndFlush(entity));
    }

    /** Hides the row from pickers. History keeps it, and activate brings it back. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public D deactivate(UUID id) {
        var entity = load(id);
        entity.setActive(false);
        return toDto(entity);
    }

    /** The mirror of deactivate. On a live row it changes nothing and still answers 200. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public D activate(UUID id) {
        var entity = load(id);
        entity.setActive(true);
        return toDto(entity);
    }

    /**
     * Names are unique per practice whether the row is active or not: a name that looks free but belongs to
     * a deactivated row is reactivated, never duplicated, because part 10 and part 14 rows point at the id.
     */
    private void requireNameFree(String name, UUID excludeId) {
        if (name == null || name.isBlank()) {
            return;   // bean validation already refused it
        }
        if (repository.countByName(name.trim(), excludeId) > 0) {
            throw new DomainException(org.springframework.http.HttpStatus.CONFLICT, "reference.name_taken", "name");
        }
    }

    protected E load(UUID id) {
        return repository.findById(id).orElseThrow(() -> DomainException.notFound("reference.not_found"));
    }

    /** A new, empty row of the concrete type. */
    protected abstract E newEntity();

    /** Copy the request onto the row; the mapper never touches id, practiceId, active, or the timestamps. */
    protected abstract void apply(R request, E entity);

    protected abstract D toDto(E entity);

    /** The name the request carries, for the uniqueness check before the row is touched. */
    protected abstract String name(R request);
}
