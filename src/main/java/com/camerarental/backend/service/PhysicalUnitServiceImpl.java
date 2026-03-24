package com.camerarental.backend.service;

import com.camerarental.backend.exceptions.ApiException;
import com.camerarental.backend.exceptions.ResourceNotFoundException;
import com.camerarental.backend.model.entity.InventoryItem;
import com.camerarental.backend.model.entity.PhysicalUnit;
import com.camerarental.backend.payload.PhysicalUnitDTO;
import com.camerarental.backend.payload.base.PagedResponse;
import com.camerarental.backend.repository.InventoryItemRepository;
import com.camerarental.backend.repository.PhysicalUnitRepository;
import com.camerarental.backend.util.PaginationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of {@link PhysicalUnitService}.
 *
 * <p>Each physical unit is a real camera body sitting on the shelf.
 * This service enforces serial-number uniqueness and validates that
 * the parent {@link InventoryItem} exists before any mutation.</p>
 *
 * <h3>N + 1 query prevention</h3>
 *
 * <p>Entity-to-DTO mapping is done <em>manually</em> rather than via
 * {@code ModelMapper} to guarantee that only the fields we explicitly
 * read are touched.  {@code ModelMapper} introspects the full object
 * graph by default; if it traverses into the lazy
 * {@code PhysicalUnit.inventoryItem} proxy it would trigger a
 * {@code SELECT} per row — a silent N + 1 that is difficult to detect
 * without SQL logging.</p>
 *
 * <p>As a second line of defence, the repository query
 * {@link PhysicalUnitRepository#findByInventoryItemInventoryItemId}
 * uses {@code @EntityGraph(attributePaths = "inventoryItem")} so the
 * parent is fetched in the same SQL join.  Even if future DTO changes
 * require additional fields from the parent, no extra query is fired.</p>
 */
@Service
@RequiredArgsConstructor
public class PhysicalUnitServiceImpl implements PhysicalUnitService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "physicalUnitId", "serialNumber", "condition", "status", "acquiredDate", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "physicalUnitId";

    private final PhysicalUnitRepository physicalUnitRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PaginationHelper paginationHelper;

    @Override
    @Transactional
    public PhysicalUnitDTO create(PhysicalUnitDTO dto) {
        UUID inventoryItemId = dto.getInventoryItemId();

        InventoryItem parent = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "inventoryItemId", inventoryItemId));

        if (physicalUnitRepository.existsBySerialNumber(dto.getSerialNumber())) {
            throw new ApiException("A physical unit with serial number '" + dto.getSerialNumber() + "' already exists.");
        }

        PhysicalUnit entity = new PhysicalUnit();
        entity.setInventoryItem(parent);
        entity.setSerialNumber(dto.getSerialNumber());
        entity.setCondition(dto.getCondition());
        entity.setStatus(dto.getStatus());
        entity.setNotes(dto.getNotes());
        entity.setAcquiredDate(dto.getAcquiredDate());

        PhysicalUnit saved = physicalUnitRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PhysicalUnitDTO> getByInventoryItem(UUID inventoryItemId,
                                                             Integer pageNumber, Integer pageSize,
                                                             String sortBy, String sortOrder) {
        if (!inventoryItemRepository.existsById(inventoryItemId)) {
            throw new ResourceNotFoundException("InventoryItem", "inventoryItemId", inventoryItemId);
        }

        Pageable pageable = paginationHelper.buildPageable(
                pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD);

        Page<PhysicalUnit> page = physicalUnitRepository
                .findByInventoryItemInventoryItemId(inventoryItemId, pageable);

        return PagedResponse.from(page, this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PhysicalUnitDTO getById(UUID physicalUnitId) {
        PhysicalUnit entity = physicalUnitRepository.findById(physicalUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("PhysicalUnit", "physicalUnitId", physicalUnitId));
        return toDto(entity);
    }

    @Override
    @Transactional
    public PhysicalUnitDTO update(UUID physicalUnitId, PhysicalUnitDTO dto) {
        PhysicalUnit existing = physicalUnitRepository.findById(physicalUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("PhysicalUnit", "physicalUnitId", physicalUnitId));

        if (!existing.getSerialNumber().equals(dto.getSerialNumber())
                && physicalUnitRepository.existsBySerialNumber(dto.getSerialNumber())) {
            throw new ApiException("A physical unit with serial number '" + dto.getSerialNumber() + "' already exists.");
        }

        existing.setSerialNumber(dto.getSerialNumber());
        existing.setCondition(dto.getCondition());
        existing.setStatus(dto.getStatus());
        existing.setNotes(dto.getNotes());
        existing.setAcquiredDate(dto.getAcquiredDate());

        PhysicalUnit saved = physicalUnitRepository.save(existing);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(UUID physicalUnitId) {
        PhysicalUnit entity = physicalUnitRepository.findById(physicalUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("PhysicalUnit", "physicalUnitId", physicalUnitId));
        physicalUnitRepository.delete(entity);
    }

    /**
     * Manual entity-to-DTO mapping.
     *
     * <p>Deliberately avoids {@code ModelMapper} to prevent accidental
     * lazy-proxy traversal into {@code inventoryItem} (and beyond into
     * {@code camera}).  Only the FK-backed {@code inventoryItemId} is
     * read — an operation that does <em>not</em> initialise the
     * Hibernate proxy in Hibernate 6+, because the ID is already set
     * from the foreign-key column.</p>
     */
    private PhysicalUnitDTO toDto(PhysicalUnit entity) {
        PhysicalUnitDTO dto = new PhysicalUnitDTO();
        dto.setPhysicalUnitId(entity.getPhysicalUnitId());
        dto.setInventoryItemId(entity.getInventoryItem().getInventoryItemId());
        dto.setSerialNumber(entity.getSerialNumber());
        dto.setCondition(entity.getCondition());
        dto.setStatus(entity.getStatus());
        dto.setNotes(entity.getNotes());
        dto.setAcquiredDate(entity.getAcquiredDate());
        return dto;
    }
}
