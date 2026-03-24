package com.camerarental.backend.repository;

import com.camerarental.backend.model.entity.Camera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Data-access layer for {@link Camera} entities.
 *
 * <p>Provides a uniqueness check on model name (used when creating or
 * updating a camera) and a case-insensitive search across brand and
 * model name for the paginated camera listing endpoint.</p>
 */
@Repository
public interface CameraRepository extends JpaRepository<Camera, UUID> {

    boolean existsByModelName(String modelName);

    /**
     * Case-insensitive partial-match search against both {@code brand} and
     * {@code modelName}.
     *
     * <p>A custom JPQL query is needed here because Spring Data's
     * derived query naming ({@code findByBrandContainingIgnoreCase...})
     * does not support an OR across two columns with a single search
     * term while also returning a {@link Page}. The explicit
     * {@code LOWER(... LIKE ...)} approach keeps the search
     * database-agnostic and lets us wrap the term in wildcards for
     * substring matching.</p>
     */
    @Query("SELECT c FROM Camera c WHERE LOWER(c.brand) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.modelName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Camera> searchByBrandOrModelName(@Param("search") String search, Pageable pageable);

    Page<Camera> findAllByIsActiveTrue(Pageable pageable);

    @Query("SELECT c FROM Camera c WHERE c.isActive = true AND " +
           "(LOWER(c.brand) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.modelName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Camera> searchActiveByBrandOrModelName(@Param("search") String search, Pageable pageable);
}
