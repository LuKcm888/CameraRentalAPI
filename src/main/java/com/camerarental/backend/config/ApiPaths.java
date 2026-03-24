package com.camerarental.backend.config;

/**
 * Centralised REST path constants.
 *
 * <p>Every {@code @RequestMapping} in the application references these
 * constants so that a URL change only requires a single edit here rather
 * than a find-and-replace across controllers.</p>
 */
public class ApiPaths {

    private ApiPaths() {}

    public static final String API_BASE = "/api/v1";

    // Resource prefixes
    public static final String AUTH = API_BASE + "/auth";
    public static final String CAMERAS = API_BASE + "/cameras";
    public static final String BUSINESS_HOURS = API_BASE + "/business-hours";
    public static final String INVENTORY = API_BASE + "/inventory";
    public static final String PHYSICAL_UNITS = API_BASE + "/units";

}
