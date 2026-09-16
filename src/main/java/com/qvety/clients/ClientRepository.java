package com.qvety.clients;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** RLS scopes every query to the caller's practice. */
public interface ClientRepository extends JpaRepository<Client, UUID> {

    /** Plain list; derived so an unknown sort property is a 400 (`bad_sort`), not a Hibernate error. */
    Page<Client> findByArchivedAtIsNull(Pageable pageable);

    /**
     * Name branch: substring or trigram match on the folded name, best match first. `%` is the pg_trgm
     * similarity operator (threshold `pg_trgm.similarity_threshold`, default 0.3) and, unlike
     * `similarity(...) > 0.3`, it uses the GIN index. Exact email kept from part 06. Native because
     * JPQL has neither ILIKE nor the operator; the Pageable must carry no sort.
     */
    @Query(value = """
        SELECT * FROM clients
        WHERE (:includeArchived OR archived_at IS NULL)
          AND (full_name_normalized ILIKE '%' || :q || '%' OR full_name_normalized % :q OR email = :q)
        ORDER BY similarity(full_name_normalized, :q) DESC, full_name_normalized
        """, countQuery = """
        SELECT count(*) FROM clients
        WHERE (:includeArchived OR archived_at IS NULL)
          AND (full_name_normalized ILIKE '%' || :q || '%' OR full_name_normalized % :q OR email = :q)
        """, nativeQuery = true)
    Page<Client> searchByName(@Param("q") String q, @Param("includeArchived") boolean includeArchived, Pageable pageable);

    /** Phone branch: the search term parsed as E.164, matched against either phone column. */
    @Query("""
        SELECT c FROM Client c
        WHERE (:includeArchived = true OR c.archivedAt IS NULL)
          AND (c.phoneE164 = :e164 OR c.phoneSecondaryE164 = :e164)
        """)
    Page<Client> searchByPhone(@Param("e164") String e164, @Param("includeArchived") boolean includeArchived, Pageable pageable);

    /**
     * Duplicate candidates: any of the saved E.164 phones in either column, or the same folded name. Archived
     * clients stay out: the desk cannot act on them without unarchiving first, and then the row warns on its own.
     */
    @Query("""
        SELECT c FROM Client c
        WHERE c.archivedAt IS NULL
          AND (c.id <> :excludeId OR :excludeId IS NULL)
          AND ((:phone IS NOT NULL AND (c.phoneE164 = :phone OR c.phoneSecondaryE164 = :phone))
               OR (:phone2 IS NOT NULL AND (c.phoneE164 = :phone2 OR c.phoneSecondaryE164 = :phone2))
               OR c.fullNameNormalized = :name)
        """)
    List<Client> findDuplicateCandidates(@Param("name") String fullNameNormalized, @Param("phone") String phoneE164,
                                         @Param("phone2") String phoneSecondaryE164, @Param("excludeId") UUID excludeId);
}
