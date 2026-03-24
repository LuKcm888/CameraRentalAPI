package com.camerarental.backend.service;

import com.camerarental.backend.payload.InventoryItemDTO;
import com.camerarental.backend.payload.base.PagedResponse;

import java.util.UUID;

/**
 * Contract for {@link com.camerarental.backend.model.entity.InventoryItem}
 * CRUD operations.
 *
 * <p>An inventory item links a {@code Camera} catalog entry to its
 * pricing and the physical units the rental house owns.  The derived
 * counts ({@code totalUnits}, {@code availableUnits}) are computed from
 * the associated {@code PhysicalUnit} rows, not stored redundantly.</p>
 */
public interface InventoryItemService {

    /**
     * Creates an inventory item for a camera that does not yet have one.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if the referenced camera does not exist
     * @throws com.camerarental.backend.exceptions.ApiException
     *         if the camera already has an inventory item
     */
    InventoryItemDTO create(InventoryItemDTO dto);

    /**
     * Returns a paginated list of all inventory items with computed unit counts.
     */
    PagedResponse<InventoryItemDTO> getAll(Integer pageNumber, Integer pageSize,
                                           String sortBy, String sortOrder);

    /**
     * Returns a single inventory item by ID with computed unit counts.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if no item exists with the given ID
     */
    InventoryItemDTO getById(UUID inventoryItemId);

    /**
     * Updates pricing on an existing inventory item.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if the item does not exist
     */
    InventoryItemDTO update(UUID inventoryItemId, InventoryItemDTO dto);

    /**
     * Deletes an inventory item and all its physical units (cascade).
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if the item does not exist
     */
    void delete(UUID inventoryItemId);
}
