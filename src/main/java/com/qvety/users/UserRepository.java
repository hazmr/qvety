package com.qvety.users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPracticeIdAndEmailIgnoreCase(UUID practiceId, String email);

    /** Login has no practice yet: look the email up across practices. First active match wins. */
    List<User> findByEmailIgnoreCase(String email);

    List<User> findByPracticeIdOrderByFullName(UUID practiceId);

    Optional<User> findByIdAndPracticeId(UUID id, UUID practiceId);
}
