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
     * @param search    optional keyword matched against brand and model name
     * @param sortBy    entity field to sort on (whitelist-validated)
     * @param sortOrder {@code "asc"} or {@code "desc"}
     */
    PagedResponse<CameraDTO> getCameras(String search, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

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
     * Deletes a camera by its ID.
     *
     * @throws com.camerarental.backend.exceptions.ResourceNotFoundException
     *         if no camera exists with the given ID
     */
    void deleteCamera(UUID cameraId);
}
