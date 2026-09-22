package com.qvety.scheduling;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** RLS scopes every query to the caller's practice. */
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /**
     * One day's appointments. The five references are fetched in one go: the day grid names every one of
     * them, so leaving them lazy would be a select per row per reference.
     */
    @Query("""
        SELECT a FROM Appointment a
          JOIN FETCH a.patient
          JOIN FETCH a.client
          JOIN FETCH a.veterinarian
          JOIN FETCH a.room
          JOIN FETCH a.appointmentType
        WHERE a.startsAt >= :from AND a.startsAt < :to
          AND (:veterinarianId IS NULL OR a.veterinarian.id = :veterinarianId)
        ORDER BY a.startsAt, a.veterinarian.id
        """)
    List<Appointment> findDay(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                              @Param("veterinarianId") UUID veterinarianId);

    /**
     * The board. A native query because it computes minutes waited in the database and returns only the
     * ten columns a card shows, rather than five entity graphs the board never uses.
     */
    @Query(value = """
        SELECT a.id,
               p.name                                                        AS patient_name,
               c.full_name                                                   AS client_name,
               u.full_name                                                   AS veterinarian_name,
               r.name                                                        AS room_name,
               a.starts_at,
               a.status::text                                                AS status,
               a.origin::text                                                AS origin,
               CASE WHEN a.checked_in_at IS NULL THEN NULL
                    ELSE floor(EXTRACT(EPOCH FROM (now() - a.checked_in_at)) / 60)::int END AS minutes_waiting,
               a.reason
        FROM appointments a
          JOIN patients p ON p.id = a.patient_id
          JOIN clients  c ON c.id = a.client_id
          JOIN users    u ON u.id = a.veterinarian_id
          JOIN rooms    r ON r.id = a.room_id
        WHERE a.starts_at >= :from AND a.starts_at < :to
          AND a.status IN ('checked_in', 'in_progress', 'completed')
        ORDER BY a.checked_in_at NULLS LAST, a.starts_at
        """, nativeQuery = true)
    List<BoardRow> findBoard(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /**
     * Projection for the board query; Spring Data maps the column aliases onto these accessors. The
     * driver hands a timestamptz back as an Instant, and an interface projection does no conversion,
     * so starts_at is read as one and given its offset by the caller.
     */
    interface BoardRow {
        UUID getId();
        String getPatientName();
        String getClientName();
        String getVeterinarianName();
        String getRoomName();
        Instant getStartsAt();
        String getStatus();
        String getOrigin();
        Integer getMinutesWaiting();
        String getReason();
    }
}
