package com.camerarental.backend.model.entity;

import com.camerarental.backend.model.base.AuditableEntity;
import com.camerarental.backend.model.entity.enums.UnitCondition;
import com.camerarental.backend.model.entity.enums.UnitStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

import static com.camerarental.backend.config.ValidationConstraints.*;

/**
 * A single physical camera unit owned by the rental house.
 *
 * <p>Each unit belongs to an {@link InventoryItem} and is individually
 * tracked by serial number, condition, and availability status.  This
 * granularity enables the business to know exactly which unit a customer
 * received and to track wear over time.</p>
 */
@Entity
@Table(name = "physical_unit", uniqueConstraints = {
        @UniqueConstraint(name = "uk_physical_unit_serial", columnNames = "serial_number")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PhysicalUnit extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "physical_unit_id")
    @EqualsAndHashCode.Include
    private UUID physicalUnitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "serial_number", nullable = false, length = UNIT_SERIAL_NUMBER_MAX)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_condition", nullable = false, length = UNIT_CONDITION_MAX)
    private UnitCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_status", nullable = false, length = UNIT_STATUS_MAX)
    private UnitStatus status;

    @Column(length = UNIT_NOTES_MAX)
    private String notes;

    @Column(name = "acquired_date")
    private LocalDate acquiredDate;
}
