package com.camerarental.backend.payload;

import com.camerarental.backend.model.entity.enums.UnitCondition;
import com.camerarental.backend.model.entity.enums.UnitStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

import static com.camerarental.backend.config.ValidationConstraints.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhysicalUnitDTO {

    private UUID physicalUnitId;

    @NotNull
    private UUID inventoryItemId;

    @NotBlank
    @Size(min = UNIT_SERIAL_NUMBER_MIN, max = UNIT_SERIAL_NUMBER_MAX)
    private String serialNumber;

    @NotNull
    private UnitCondition condition;

    @NotNull
    private UnitStatus status;

    @Size(min = UNIT_NOTES_MIN, max = UNIT_NOTES_MAX)
    private String notes;

    private LocalDate acquiredDate;
}
