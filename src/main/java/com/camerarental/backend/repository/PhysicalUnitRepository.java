package com.camerarental.backend.repository;

import com.camerarental.backend.model.entity.PhysicalUnit;
import com.camerarental.backend.model.entity.enums.UnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link PhysicalUnit}.
 *
 * <p>Supports filtered queries by inventory item and status, plus
 * aggregate counts used to compute availability on the
 * {@link com.camerarental.backend.payload.InventoryItemDTO}.</p>
 *
 * <h3>N + 1 prevention — batched unit counts</h3>
 *
 * <p>The inventory-item list endpoint needs two derived values for
 * every row: <em>totalUnits</em> and <em>availableUnits</em>.  Calling
 * {@link #countByInventoryItemInventoryItemId} and
 * {@link #countByInventoryItemInventoryItemIdAndStatus} per item would
 * produce 2 × N additional SELECTs — a textbook N + 1 pattern.</p>
 *
 * <p>{@link #batchCountsByInventoryItemIds(Collection)} collapses those
 * 2N queries into a <strong>single</strong> aggregate query that returns
 * both counts for every requested inventory item in one round-trip.</p>
 */
@Repository
public interface PhysicalUnitRepository extends JpaRepository<PhysicalUnit, UUID> {

    /**
     * Paginated listing of units for a single inventory item.
     *
     * <p>An {@link EntityGraph} eagerly loads the parent
     * {@code inventoryItem} so that DTO mapping can read the
     * {@code inventoryItemId} without triggering a lazy proxy
     * initialisation per row.  This is a <em>defensive</em> measure:
     * Hibernate 6 does not initialise a proxy when only the ID is
     * accessed, but future DTO changes or ModelMapper traversals
     * could silently introduce an N + 1 if the graph were absent.</p>
     */
    @EntityGraph(attributePaths = "inventoryItem")
    Page<PhysicalUnit> findByInventoryItemInventoryItemId(UUID inventoryItemId, Pageable pageable);

    boolean existsBySerialNumber(String serialNumber);

    long countByInventoryItemInventoryItemId(UUID inventoryItemId);

    long countByInventoryItemInventoryItemIdAndStatus(UUID inventoryItemId, UnitStatus status);

    /**
     * Returns total and available unit counts for a batch of inventory
     * items in a single SQL round-trip.
     *
     * <p>Each element in the returned list is an {@code Object[]} of
     * three values:</p>
     * <ol>
     *   <li>{@code UUID}  — the inventory-item ID</li>
     *   <li>{@code Long}  — total physical units for that item</li>
     *   <li>{@code Long}  — units with status {@code AVAILABLE}</li>
     * </ol>
     *
     * <p><strong>Inventory items with zero units will not appear in the
     * result set</strong> (the {@code INNER JOIN} eliminates them).
     * Callers must default missing IDs to {@code {0, 0}}.</p>
     *
     * <p>This query replaces 2 × N individual {@code COUNT} calls with
     * one grouped aggregate, reducing the list endpoint from
     * O(N) queries to O(1).</p>
     */
    @Query("""
            SELECT pu.inventoryItem.inventoryItemId,
                   COUNT(pu),
                   COUNT(CASE WHEN pu.status = 'AVAILABLE' THEN 1 END)
            FROM PhysicalUnit pu
            WHERE pu.inventoryItem.inventoryItemId IN :ids
            GROUP BY pu.inventoryItem.inventoryItemId
            """)
    List<Object[]> batchCountsByInventoryItemIds(@Param("ids") Collection<UUID> ids);
}
