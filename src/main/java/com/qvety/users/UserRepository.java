package com.qvety.users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPracticeIdAndEmailIgnoreCase(UUID practiceId, String email);

    /**
     * Login has no tenant yet, and users is under forced RLS, so this goes through the definer
     * function login_lookup(): active users with this email across practices. First match wins.
     */
    @Query(value = "SELECT * FROM login_lookup(:email)", nativeQuery = true)
    List<User> findForLogin(@Param("email") String email);

    List<User> findByPracticeIdOrderByFullName(UUID practiceId);

    Optional<User> findByIdAndPracticeId(UUID id, UUID practiceId);
}
