package com.camerarental.backend.payload;

import com.camerarental.backend.model.entity.enums.CameraCategory;
import com.camerarental.backend.model.entity.enums.SensorFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static com.camerarental.backend.config.ValidationConstraints.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CameraDTO {
    private UUID cameraId;
    @NotBlank
    @Size(min = CAMERA_BRAND_MIN, max = CAMERA_BRAND_MAX)
    private String brand;
    @NotBlank
    @Size(min = CAMERA_MODEL_NAME_MIN, max = CAMERA_MODEL_NAME_MAX)
    private String modelName;
    @NotNull
    private CameraCategory category;
    @NotNull
    private SensorFormat sensorFormat;

    private String lensMount;

    private boolean active;

    private boolean videoCapable;

    private boolean photoCapable;

    @Size(min = CAMERA_RESOLUTION_MIN, max = CAMERA_RESOLUTION_MAX)
    private String resolution;

    private Integer maxIso;

    private Integer maxFrameRate4k;

    private Integer maxFrameRate1080p;

    @Size(min = CAMERA_DESCRIPTION_MIN, max = CAMERA_DESCRIPTION_MAX)
    private String description;
}
