package com.camerarental.backend.service;

import com.camerarental.backend.payload.PhysicalUnitDTO;
import com.camerarental.backend.payload.base.PagedResponse;

import java.util.UUID;

/**
 * Contract for {@link com.camerarental.backend.model.entity.PhysicalUnit}
 * CRUD operations.
 *
 * <p>Physical units are always scoped to an
 * {@link com.camerarental.backend.model.entity.InventoryItem}.  The
 * list endpoint is filtered by inventory item ID so that callers see
 * only the units belonging to a particular camera model.</p>
 */
public interface PhysicalUnitService {

    /**
     * Registers a new physical unit under an existing inventory item.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if the referenced inventory item does not exist
     * @throws com.camerarental.backend.exceptions.ApiException
     *         if the serial number is already in use
     */
    PhysicalUnitDTO create(PhysicalUnitDTO dto);

    /**
     * Returns a paginated list of physical units for a given inventory item.
     */
    PagedResponse<PhysicalUnitDTO> getByInventoryItem(UUID inventoryItemId,
                                                      Integer pageNumber, Integer pageSize,
                                                      String sortBy, String sortOrder);

    /**
     * Returns a single physical unit by its ID.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if the unit does not exist
     */
    PhysicalUnitDTO getById(UUID physicalUnitId);

    /**
     * Updates a physical unit's mutable fields (serial number, condition,
     * status, notes, acquired date).
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if the unit does not exist
     * @throws com.camerarental.backend.exceptions.ApiException
     *         if the new serial number collides with another unit
     */
    PhysicalUnitDTO update(UUID physicalUnitId, PhysicalUnitDTO dto);

    /**
     * Deletes a physical unit by its ID.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if the unit does not exist
     */
    void delete(UUID physicalUnitId);
}
