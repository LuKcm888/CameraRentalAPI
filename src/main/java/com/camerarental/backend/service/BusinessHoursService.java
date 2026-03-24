package com.camerarental.backend.service;

import com.camerarental.backend.payload.BusinessHoursDTO;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Contract for business-hours CRUD operations.
 *
 * <p>The schedule is a fixed set of seven rows (one per day of the week).
 * All lookups, updates, and deletes are keyed by {@link DayOfWeek} rather
 * than an internal UUID, since the day is the natural identifier callers
 * know.</p>
 */
public interface BusinessHoursService {

    /**
     * Creates a business-hours entry for a day that does not yet exist.
     *
     * @throws com.camerarental.backend.exceptions.ApiException if the
     *         day already has an entry
     */
    BusinessHoursDTO create(BusinessHoursDTO dto);

    /** Returns the full weekly schedule ordered by day of week. */
    List<BusinessHoursDTO> getAll();

    /**
     * Returns a single day's hours.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if no entry exists for the given day
     */
    BusinessHoursDTO getByDay(DayOfWeek day);

    /**
     * Updates an existing day's hours (open/close times and closed flag).
     *
     * @param day the day to update
     * @param dto new values (the {@code dayOfWeek} field in the body is ignored)
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if no entry exists for the given day
     */
    BusinessHoursDTO update(DayOfWeek day, BusinessHoursDTO dto);

    /**
     * Deletes a business-hours entry by day.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if no entry exists for the given day
     */
    void delete(DayOfWeek day);
}
