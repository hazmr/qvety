package com.qvety.practice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeRepository extends JpaRepository<Practice, UUID> {
}
