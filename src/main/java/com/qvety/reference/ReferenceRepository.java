package com.qvety.reference;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

/**
 * One base repository for the three reference tables; `#{#entityName}` resolves to the concrete entity.
 * RLS scopes every query to the caller's practice.
 */
@NoRepositoryBean
public interface ReferenceRepository<E extends ReferenceEntity> extends JpaRepository<E, UUID> {

    /** Live rows first, then by name, so switching inactive rows on never interleaves them. */
    @Query("""
        SELECT e FROM #{#entityName} e
        WHERE :includeInactive = true OR e.active = true
        ORDER BY e.active DESC, LOWER(e.name)
        """)
    List<E> listOrdered(@Param("includeInactive") boolean includeInactive);

    /** The unique index is the real guard; this turns the race-free common case into a clean 409. */
    @Query("""
        SELECT COUNT(e) FROM #{#entityName} e
        WHERE LOWER(e.name) = LOWER(:name) AND (:excludeId IS NULL OR e.id <> :excludeId)
        """)
    long countByName(@Param("name") String name, @Param("excludeId") UUID excludeId);
}
