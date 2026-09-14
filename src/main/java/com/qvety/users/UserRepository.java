package com.qvety.users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPracticeIdAndEmailIgnoreCase(UUID practiceId, String email);

    Optional<User> findByPracticeIdAndPhone(UUID practiceId, String phone);

    /**
     * Login has no tenant yet, and users is under forced RLS, so this goes through the definer
     * function login_lookup(): active users with this E.164 phone or email across practices.
     */
    @Query(value = "SELECT * FROM login_lookup(:identifier)", nativeQuery = true)
    List<User> findForLogin(@Param("identifier") String identifier);

    List<User> findByPracticeIdOrderByFullName(UUID practiceId);

    Optional<User> findByIdAndPracticeId(UUID id, UUID practiceId);
}
