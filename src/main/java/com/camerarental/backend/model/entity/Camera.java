package com.camerarental.backend.model.entity;

import com.camerarental.backend.model.base.AuditableEntity;
import com.camerarental.backend.model.entity.enums.CameraCategory;
import com.camerarental.backend.model.entity.enums.SensorFormat;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

import static com.camerarental.backend.config.ValidationConstraints.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Camera extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "camera_id")
    @EqualsAndHashCode.Include
    private UUID cameraId;

    @Column(nullable = false, length = CAMERA_BRAND_MAX)
    private String brand;

    @Column(name = "model_name", nullable = false, length = CAMERA_MODEL_NAME_MAX)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_category", nullable = false, length = CAMERA_CATEGORY_MAX)
    private CameraCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_format", nullable = false, length = CAMERA_SENSOR_FORMAT_MAX)
    private SensorFormat sensorFormat;

    @Column(name = "lens_mount", length = CAMERA_LENS_MOUNT_MAX)
    private String lensMount;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_video_capable", nullable = false)
    private boolean isVideoCapable = true;

    @Column(name = "is_photo_capable", nullable = false)
    private boolean isPhotoCapable = true;

    @Column(length = CAMERA_RESOLUTION_MAX)
    private String resolution;

    @Column(name = "max_iso")
    private Integer maxIso;

    @Column(name = "max_frame_rate_4k")
    private Integer maxFrameRate4k;

    @Column(name = "max_frame_rate_1080p")
    private Integer maxFrameRate1080p;

    @Column(name = "description", length = CAMERA_DESCRIPTION_MAX)
    private String description;
    
}
