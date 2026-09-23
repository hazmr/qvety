package com.qvety.platform;

import com.qvety.practice.Practice;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The platform's door to `practices`. The table is under forced row-level security keyed on the current
 * practice, and part 04 withheld UPDATE on status from the application role on purpose, so every call
 * here goes through a definer function owned by qvety_owner. The application role holds EXECUTE on those
 * three functions and no rights on the table itself, so no other code path can change a practice.
 */
public interface PlatformPracticeRepository extends JpaRepository<Practice, UUID> {

    @Query(value = "SELECT * FROM platform_practices()", nativeQuery = true)
    List<Practice> findEveryPractice();

    @Query(value = "SELECT * FROM platform_practices() WHERE id = :id", nativeQuery = true)
    List<Practice> findOnePractice(@Param("id") UUID id);

    @Query(value = "SELECT platform_practice_create(:name, CAST(:country AS char(2)), CAST(:currency AS char(3)), :locale, :timezone, :trialDays)",
           nativeQuery = true)
    UUID createPractice(@Param("name") String name, @Param("country") String country,
                        @Param("currency") String currency, @Param("locale") String locale,
                        @Param("timezone") String timezone, @Param("trialDays") int trialDays);

    @Query(value = "SELECT * FROM platform_practice_set_status(:id, CAST(:status AS practice_status))",
           nativeQuery = true)
    List<Practice> setStatus(@Param("id") UUID id, @Param("status") String status);
}
