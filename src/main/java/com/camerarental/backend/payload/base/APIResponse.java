package com.camerarental.backend.payload.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This DTO class is used to standardize exception responses in the application.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class APIResponse {
    private String message;
    private boolean status;
}
