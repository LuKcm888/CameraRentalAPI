package com.camerarental.backend.config;

/**
 * Default values for paginated API endpoints.
 *
 * <p>Controllers reference these constants via
 * {@code @RequestParam(defaultValue = ...)} so that callers who omit
 * pagination parameters get a sensible default page.</p>
 */
public class AppConstants {

    private AppConstants() {}

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE  = "50";
    public static final String SORT_CAMERAS_BY = "cameraId";
    public static final String SORT_INVENTORY_BY = "inventoryItemId";
    public static final String SORT_UNITS_BY = "physicalUnitId";
    public static final String SORT_DIR = "asc";
}
