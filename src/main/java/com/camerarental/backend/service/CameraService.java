package com.camerarental.backend.service;

import com.camerarental.backend.payload.CameraDTO;
import com.camerarental.backend.payload.base.PagedResponse;

import java.util.UUID;

/**
 * Contract for camera CRUD operations.
 *
 * <p>All methods work exclusively with {@link CameraDTO} so that the
 * controller layer never touches JPA entities directly.</p>
 */
public interface CameraService {

    /**
     * Creates a new camera after verifying the model name is unique.
     *
     * @param currentUserId UUID of the authenticated admin performing the action
     * @param dto           camera data to persist
     * @return the persisted camera with its generated ID
     * @throws com.camerarental.backend.exceptions.ApiException if a camera
     *         with the same model name already exists
     */
    CameraDTO createCamera(UUID currentUserId, CameraDTO dto);

    /**
     * Retrieves a single camera by its ID.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if no camera exists with the given ID
     */
    CameraDTO getCamera(UUID cameraId);

    /**
     * Returns a paginated, optionally filtered list of cameras.
     *
     * <p>By default only active cameras are returned.  Admins can pass
     * {@code includeInactive = true} to see the full catalog, including
     * cameras that have been soft-deleted / deactivated.</p>
     *
     * @param search          optional keyword matched against brand and model name
     * @param includeInactive when {@code true}, deactivated cameras are included
     * @param sortBy          entity field to sort on (whitelist-validated)
     * @param sortOrder       {@code "asc"} or {@code "desc"}
     */
    PagedResponse<CameraDTO> getCameras(String search, boolean includeInactive,
                                        Integer pageNumber, Integer pageSize,
                                        String sortBy, String sortOrder);

    /**
     * Replaces every mutable field on an existing camera.
     *
     * @param currentUserId UUID of the authenticated admin performing the action
     * @param cameraId      ID of the camera to update
     * @param dto           new field values
     * @return the updated camera
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if the camera does not exist
     * @throws com.camerarental.backend.exceptions.ApiException if the new
     *         model name collides with another camera
     */
    CameraDTO updateCamera(UUID currentUserId, UUID cameraId, CameraDTO dto);

    /**
     * Permanently removes a camera and its empty inventory record from the
     * database.
     *
     * <h3>Why this is rarely used</h3>
     *
     * <p>In a real rental business, hard-deleting a catalog item is almost
     * never the right call.  Cameras that have been rented, insured, or
     * depreciated carry historical data (rental agreements, maintenance
     * logs, revenue reports) that becomes orphaned when the catalog entry
     * disappears.  The strongly preferred path is
     * {@link #deactivateCamera(UUID)}, which sets {@code isActive = false}
     * and hides the camera from customer-facing listings while preserving
     * all audit trails.</p>
     *
     * <p>Hard-delete exists only as a <strong>data-cleanup</strong>
     * operation — for example, removing a camera that was entered by
     * mistake or a duplicate record.</p>
     *
     * <h3>Safety checks</h3>
     *
     * <p>Before deleting, the service verifies that <em>no physical units
     * are on record</em> for this camera's inventory.  If units exist the
     * call is rejected with a {@code 409 Conflict} so the admin knows to
     * retire or remove units first.  If an inventory record exists but has
     * zero units, it is cascade-deleted automatically.</p>
     *
     * @param cameraId the camera to delete
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if no camera exists with the given ID
     * @throws com.camerarental.backend.exceptions.ResourceConflictException
     *         if physical units still reference this camera's inventory
     */
    void deleteCamera(UUID cameraId);

    /**
     * Soft-deletes a camera by setting {@code isActive = false}.
     *
     * <p>This is the <strong>recommended</strong> way to retire a camera
     * from the catalog.  The record stays in the database for reporting,
     * audit, and historical rental references, but it no longer appears
     * in customer-facing queries.</p>
     *
     * @param cameraId the camera to deactivate
     * @return the updated camera DTO with {@code active = false}
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if no camera exists with the given ID
     */
    CameraDTO deactivateCamera(UUID cameraId);
}
