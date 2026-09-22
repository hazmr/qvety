package com.qvety.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeHoursRepository extends JpaRepository<PracticeHours, UUID> {

    Optional<PracticeHours> findByWeekday(short weekday);

    List<PracticeHours> findAllByOrderByWeekdayAsc();
}
