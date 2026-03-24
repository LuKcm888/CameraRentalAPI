package com.camerarental.backend.payload;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryItemDTO {

    private UUID inventoryItemId;

    @NotNull
    private UUID cameraId;

    private String cameraBrand;

    private String cameraModelName;

    @NotNull
    @Positive
    private BigDecimal dailyRentalPrice;

    @NotNull
    @Positive
    private BigDecimal replacementValue;

    private Long totalUnits;

    private Long availableUnits;
}
