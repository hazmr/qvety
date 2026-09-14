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

    Page<Client> findByArchivedAtIsNull(Pageable pageable);

    /**
     * Part 06 search: case-insensitive substring on the name or exact phone/email. Part 07 replaces it
     * with the normalized columns and trigram similarity.
     */
    @Query("""
        SELECT c FROM Client c
        WHERE c.archivedAt IS NULL
          AND (lower(c.fullName) LIKE lower(concat('%', :q, '%'))
               OR c.phone = :q OR c.phoneSecondary = :q OR lower(c.email) = lower(:q))
        """)
    Page<Client> search(@Param("q") String q, Pageable pageable);

    /** Duplicate candidates: same phone in either column, or the same name after trim and case fold. */
    @Query("""
        SELECT c FROM Client c
        WHERE c.archivedAt IS NULL
          AND (c.id <> :excludeId OR :excludeId IS NULL)
          AND ((:phone IS NOT NULL AND (c.phone = :phone OR c.phoneSecondary = :phone))
               OR lower(trim(c.fullName)) = lower(trim(:fullName)))
        """)
    List<Client> findDuplicateCandidates(@Param("fullName") String fullName, @Param("phone") String phone,
                                         @Param("excludeId") UUID excludeId);
}
