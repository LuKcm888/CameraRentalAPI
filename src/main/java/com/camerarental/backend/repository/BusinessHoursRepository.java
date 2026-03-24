package com.camerarental.backend.repository;

import com.camerarental.backend.model.entity.BusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data-access layer for {@link BusinessHours} entities.
 *
 * <p>Provides lookup by day of week and an ordered listing of all
 * seven days for the public-facing schedule endpoint.</p>
 */
@Repository
public interface BusinessHoursRepository extends JpaRepository<BusinessHours, UUID> {

    Optional<BusinessHours> findByDayOfWeek(DayOfWeek dayOfWeek);

    boolean existsByDayOfWeek(DayOfWeek dayOfWeek);

    List<BusinessHours> findAllByOrderByDayOfWeekAsc();
}
