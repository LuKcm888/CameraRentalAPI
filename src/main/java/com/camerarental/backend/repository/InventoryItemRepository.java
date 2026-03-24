package com.camerarental.backend.repository;

import com.camerarental.backend.model.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link InventoryItem}.
 *
 * <p>Provides standard CRUD plus look-ups needed to enforce the
 * one-inventory-item-per-camera business rule.</p>
 *
 * <h3>N + 1 prevention</h3>
 *
 * <p>{@code InventoryItem.camera} is mapped as {@code FetchType.LAZY} to
 * avoid unnecessary joins on write paths.  However, every read DTO
 * requires the camera's brand and model name.  Without intervention the
 * list endpoint would fire one {@code SELECT} per item to initialise
 * each Camera proxy — the classic <em>N + 1</em> problem.</p>
 *
 * <p>The overridden {@link #findAll(Pageable)} and the custom
 * {@link #findWithCameraById(UUID)} methods declare an
 * {@link EntityGraph} that instructs Hibernate to {@code LEFT JOIN FETCH}
 * the camera association in the same query, collapsing N + 1 SELECTs
 * into a single statement.</p>
 */
@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    boolean existsByCameraCameraId(UUID cameraId);

    Optional<InventoryItem> findByCameraCameraId(UUID cameraId);

    /**
     * Paginated fetch of all inventory items with the {@code camera}
     * association eagerly loaded in the same SQL query.
     *
     * <p>Overrides the default {@code JpaRepository.findAll(Pageable)}
     * so that Spring Data applies the {@code @EntityGraph} automatically.
     * This avoids N lazy-load SELECTs when mapping a page of items to
     * DTOs that reference camera fields (brand, model name).</p>
     */
    @Override
    @EntityGraph(attributePaths = "camera")
    Page<InventoryItem> findAll(Pageable pageable);

    /**
     * Single-item fetch with the {@code camera} association eagerly loaded.
     *
     * <p>Used by {@code getById}, {@code update}, and other single-record
     * paths where the DTO still needs camera fields.  Eliminates the one
     * extra lazy-load SELECT that the default {@code findById} would cause.</p>
     */
    @EntityGraph(attributePaths = "camera")
    Optional<InventoryItem> findWithCameraByInventoryItemId(UUID inventoryItemId);
}
