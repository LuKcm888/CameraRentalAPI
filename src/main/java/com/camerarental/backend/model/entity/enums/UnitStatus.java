package com.camerarental.backend.model.entity.enums;

/**
 * Lifecycle status of a physical rental unit.
 *
 * <p>Only units in {@link #AVAILABLE} status can be rented out.
 * {@link #RENTED} and {@link #MAINTENANCE} units are temporarily
 * unavailable, while {@link #RETIRED} units are permanently removed
 * from the active fleet.</p>
 */
public enum UnitStatus {
    AVAILABLE,
    RENTED,
    MAINTENANCE,
    RETIRED
}
