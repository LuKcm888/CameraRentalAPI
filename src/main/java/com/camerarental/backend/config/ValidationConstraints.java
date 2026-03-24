package com.camerarental.backend.config;

/**
 * This class contains the validation constraints for the application.
 *
 * It ensures that the validation constraints are centralized and consistent
 * across the application, specifically in the DTOs and entities.
 *
 * This approach was chosen so that we can easily configure constraints and compare to the
 * frontend constraints, ensuring they match.
 */
public class ValidationConstraints {

    private ValidationConstraints() {}

    //   User Constraints
    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 20;
    public static final int EMAIL_MAX = 254;
    public static final int PASSWORD_MIN = 8;
    public static final int DTO_PASSWORD_MAX = 40;
    public static final int HASHED_PASSWORD_MAX = 225;

    // Camera Constraints
    public static final int CAMERA_MODEL_NAME_MIN = 3;
    public static final int CAMERA_MODEL_NAME_MAX = 100;
    public static final int CAMERA_BRAND_MIN = 3;
    public static final int CAMERA_BRAND_MAX = 100;
    public static final int CAMERA_CATEGORY_MIN = 3;
    public static final int CAMERA_CATEGORY_MAX = 50;
    public static final int CAMERA_SENSOR_FORMAT_MIN = 3;
    public static final int CAMERA_SENSOR_FORMAT_MAX = 50;
    public static final int CAMERA_LENS_MOUNT_MIN = 3;
    public static final int CAMERA_LENS_MOUNT_MAX = 50;
    public static final int CAMERA_RESOLUTION_MIN = 2;
    public static final int CAMERA_RESOLUTION_MAX = 50;
    public static final int CAMERA_DESCRIPTION_MIN = 3;
    public static final int CAMERA_DESCRIPTION_MAX = 1000;

    // InventoryItem Constraints
    public static final int INVENTORY_DAILY_RENTAL_PRICE_PRECISION = 10;
    public static final int INVENTORY_DAILY_RENTAL_PRICE_SCALE = 2;
    public static final int INVENTORY_REPLACEMENT_VALUE_PRECISION = 10;
    public static final int INVENTORY_REPLACEMENT_VALUE_SCALE = 2;

    // PhysicalUnit Constraints
    public static final int UNIT_SERIAL_NUMBER_MIN = 1;
    public static final int UNIT_SERIAL_NUMBER_MAX = 100;
    public static final int UNIT_CONDITION_MAX = 20;
    public static final int UNIT_STATUS_MAX = 20;
    public static final int UNIT_NOTES_MIN = 1;
    public static final int UNIT_NOTES_MAX = 500;

}
