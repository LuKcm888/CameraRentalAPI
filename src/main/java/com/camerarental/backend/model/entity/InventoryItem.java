package com.camerarental.backend.model.entity;

import com.camerarental.backend.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.camerarental.backend.config.ValidationConstraints.*;

/**
 * Represents a rentable stock-keeping entry for a specific {@link Camera} model.
 *
 * <p>While {@code Camera} is a pure catalog record (brand, specs, etc.),
 * {@code InventoryItem} captures the business side: pricing and the
 * collection of {@link PhysicalUnit}s the rental house actually owns.</p>
 *
 * <p>The one-to-one relationship with {@code Camera} is enforced by a
 * unique constraint on {@code camera_id}.</p>
 */
@Entity
@Table(name = "inventory_item", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_item_camera", columnNames = "camera_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class InventoryItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventory_item_id")
    @EqualsAndHashCode.Include
    private UUID inventoryItemId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camera_id", nullable = false)
    private Camera camera;

    @Column(name = "daily_rental_price", nullable = false,
            precision = INVENTORY_DAILY_RENTAL_PRICE_PRECISION,
            scale = INVENTORY_DAILY_RENTAL_PRICE_SCALE)
    private BigDecimal dailyRentalPrice;

    @Column(name = "replacement_value", nullable = false,
            precision = INVENTORY_REPLACEMENT_VALUE_PRECISION,
            scale = INVENTORY_REPLACEMENT_VALUE_SCALE)
    private BigDecimal replacementValue;

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhysicalUnit> physicalUnits = new ArrayList<>();
}
