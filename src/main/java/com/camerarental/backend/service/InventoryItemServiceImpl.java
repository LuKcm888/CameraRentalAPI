package com.camerarental.backend.service;

import com.camerarental.backend.exceptions.ApiException;
import com.camerarental.backend.exceptions.ResourceNotFoundException;
import com.camerarental.backend.model.entity.Camera;
import com.camerarental.backend.model.entity.InventoryItem;
import com.camerarental.backend.model.entity.enums.UnitStatus;
import com.camerarental.backend.payload.InventoryItemDTO;
import com.camerarental.backend.payload.base.PagedResponse;
import com.camerarental.backend.repository.CameraRepository;
import com.camerarental.backend.repository.InventoryItemRepository;
import com.camerarental.backend.repository.PhysicalUnitRepository;
import com.camerarental.backend.util.PaginationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Default implementation of {@link InventoryItemService}.
 *
 * <p>Manages the link between a catalog {@link Camera} and its rental
 * pricing.  Unit counts ({@code totalUnits}, {@code availableUnits})
 * are computed on the fly from the {@code physical_unit} table so they
 * are always consistent.</p>
 *
 * <h3>N + 1 query prevention</h3>
 *
 * <p>The list endpoint is the hot path most vulnerable to the N + 1
 * problem.  Without care a page of 50 inventory items would produce
 * <strong>1 + 3N = 151 SQL statements</strong>:</p>
 * <ul>
 *   <li>1 — page query for {@code inventory_item}</li>
 *   <li>N — lazy-load of each {@code Camera} proxy</li>
 *   <li>N — {@code COUNT(*)} for total units per item</li>
 *   <li>N — {@code COUNT(*)} for available units per item</li>
 * </ul>
 *
 * <p>Two techniques collapse this to <strong>three constant-time
 * queries</strong> regardless of page size:</p>
 * <ol>
 *   <li><strong>{@code @EntityGraph} on the repository</strong> —
 *       {@code InventoryItemRepository.findAll(Pageable)} is annotated
 *       with {@code @EntityGraph(attributePaths = "camera")} so Hibernate
 *       issues a single {@code LEFT JOIN FETCH} instead of N lazy
 *       selects.</li>
 *   <li><strong>Batched aggregate query</strong> —
 *       {@code PhysicalUnitRepository.batchCountsByInventoryItemIds()}
 *       computes both {@code totalUnits} and {@code availableUnits} for
 *       every item on the page in one grouped {@code SELECT}, replacing
 *       2N individual {@code COUNT} calls.</li>
 * </ol>
 *
 * <p>Single-record operations ({@code getById}, {@code create},
 * {@code update}) still use per-item counts because N = 1 and the
 * extra round-trips are negligible.</p>
 */
@Service
@RequiredArgsConstructor
public class InventoryItemServiceImpl implements InventoryItemService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "inventoryItemId", "dailyRentalPrice", "replacementValue", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "inventoryItemId";

    private final InventoryItemRepository inventoryItemRepository;
    private final CameraRepository cameraRepository;
    private final PhysicalUnitRepository physicalUnitRepository;
    private final PaginationHelper paginationHelper;

    @Override
    @Transactional
    public InventoryItemDTO create(InventoryItemDTO dto) {
        UUID cameraId = dto.getCameraId();

        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", "cameraId", cameraId));

        if (inventoryItemRepository.existsByCameraCameraId(cameraId)) {
            throw new ApiException("Inventory item for camera '" + camera.getModelName() + "' already exists.");
        }

        InventoryItem entity = new InventoryItem();
        entity.setCamera(camera);
        entity.setDailyRentalPrice(dto.getDailyRentalPrice());
        entity.setReplacementValue(dto.getReplacementValue());

        InventoryItem saved = inventoryItemRepository.save(entity);
        return toDtoWithCounts(saved);
    }

    /**
     * Returns a paginated list of inventory items.
     *
     * <p>Query plan (3 SQL statements, constant regardless of page size):</p>
     * <ol>
     *   <li>{@code SELECT ... FROM inventory_item LEFT JOIN camera} — page
     *       fetch with Camera eagerly loaded via {@code @EntityGraph}</li>
     *   <li>{@code SELECT COUNT(*) FROM inventory_item} — Spring Data
     *       pagination count</li>
     *   <li>{@code SELECT inventory_item_id, COUNT(*), COUNT(CASE ...)
     *       FROM physical_unit WHERE ... IN (:ids) GROUP BY ...} — batched
     *       unit counts for the entire page</li>
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InventoryItemDTO> getAll(Integer pageNumber, Integer pageSize,
                                                  String sortBy, String sortOrder) {
        Pageable pageable = paginationHelper.buildPageable(
                pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD);

        Page<InventoryItem> page = inventoryItemRepository.findAll(pageable);

        Map<UUID, long[]> countsMap = buildBatchedCountsMap(page.getContent());

        return PagedResponse.from(page, item -> toDto(item, countsMap));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemDTO getById(UUID inventoryItemId) {
        InventoryItem entity = inventoryItemRepository.findWithCameraByInventoryItemId(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "inventoryItemId", inventoryItemId));
        return toDtoWithCounts(entity);
    }

    @Override
    @Transactional
    public InventoryItemDTO update(UUID inventoryItemId, InventoryItemDTO dto) {
        InventoryItem existing = inventoryItemRepository.findWithCameraByInventoryItemId(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "inventoryItemId", inventoryItemId));

        existing.setDailyRentalPrice(dto.getDailyRentalPrice());
        existing.setReplacementValue(dto.getReplacementValue());

        InventoryItem saved = inventoryItemRepository.save(existing);
        return toDtoWithCounts(saved);
    }

    @Override
    @Transactional
    public void delete(UUID inventoryItemId) {
        InventoryItem entity = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "inventoryItemId", inventoryItemId));
        inventoryItemRepository.delete(entity);
    }

    // ------------------------------------------------------------------ //
    //  Mapping helpers                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Builds a lookup map of {@code {totalUnits, availableUnits}} keyed
     * by inventory-item ID using a <strong>single</strong> batched
     * aggregate query.
     *
     * <p>Items with zero physical units will not appear in the query
     * result (the {@code GROUP BY} eliminates them), so callers must
     * treat a missing key as {@code {0, 0}}.</p>
     */
    private Map<UUID, long[]> buildBatchedCountsMap(List<InventoryItem> items) {
        if (items.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> ids = items.stream()
                .map(InventoryItem::getInventoryItemId)
                .toList();

        Map<UUID, long[]> map = new HashMap<>(ids.size());
        for (Object[] row : physicalUnitRepository.batchCountsByInventoryItemIds(ids)) {
            UUID id = (UUID) row[0];
            long total = (Long) row[1];
            long available = (Long) row[2];
            map.put(id, new long[]{ total, available });
        }
        return map;
    }

    /**
     * Maps an entity to a DTO using pre-computed counts from
     * {@link #buildBatchedCountsMap}.  Used on the list path where
     * counts are fetched in bulk.
     */
    private InventoryItemDTO toDto(InventoryItem entity, Map<UUID, long[]> countsMap) {
        UUID itemId = entity.getInventoryItemId();
        Camera camera = entity.getCamera();
        long[] counts = countsMap.getOrDefault(itemId, new long[]{ 0, 0 });

        InventoryItemDTO dto = new InventoryItemDTO();
        dto.setInventoryItemId(itemId);
        dto.setCameraId(camera.getCameraId());
        dto.setCameraBrand(camera.getBrand());
        dto.setCameraModelName(camera.getModelName());
        dto.setDailyRentalPrice(entity.getDailyRentalPrice());
        dto.setReplacementValue(entity.getReplacementValue());
        dto.setTotalUnits(counts[0]);
        dto.setAvailableUnits(counts[1]);
        return dto;
    }

    /**
     * Maps a single entity to a DTO, fetching counts individually.
     *
     * <p>Used on single-record paths ({@code create}, {@code update},
     * {@code getById}) where N = 1 and the two extra {@code COUNT}
     * queries are negligible.</p>
     */
    private InventoryItemDTO toDtoWithCounts(InventoryItem entity) {
        UUID itemId = entity.getInventoryItemId();
        long total = physicalUnitRepository.countByInventoryItemInventoryItemId(itemId);
        long available = physicalUnitRepository.countByInventoryItemInventoryItemIdAndStatus(itemId, UnitStatus.AVAILABLE);
        return toDto(entity, Map.of(itemId, new long[]{ total, available }));
    }
}
