package com.camerarental.backend.service;

import com.camerarental.backend.exceptions.ApiException;
import com.camerarental.backend.exceptions.ResourceConflictException;
import com.camerarental.backend.exceptions.ResourceNotFoundException;
import com.camerarental.backend.model.entity.Camera;
import com.camerarental.backend.model.entity.InventoryItem;
import com.camerarental.backend.payload.CameraDTO;
import com.camerarental.backend.payload.base.PagedResponse;
import com.camerarental.backend.repository.CameraRepository;
import com.camerarental.backend.repository.InventoryItemRepository;
import com.camerarental.backend.repository.PhysicalUnitRepository;
import com.camerarental.backend.util.PaginationHelper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of {@link CameraService}.
 *
 * <p>Converts between {@link Camera} entities and {@link CameraDTO} objects
 * using {@link ModelMapper}, delegates persistence to
 * {@link CameraRepository}, and enforces business rules such as model-name
 * uniqueness.</p>
 *
 * <p>Sorting is restricted to a whitelist of entity fields to prevent
 * arbitrary column access via the API.</p>
 */
@Service
@RequiredArgsConstructor
public class CameraServiceImpl implements CameraService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "cameraId", "brand", "modelName", "category"
    );
    private static final String DEFAULT_SORT_FIELD = "cameraId";

    private final ModelMapper modelMapper;
    private final PaginationHelper paginationHelper;
    private final CameraRepository cameraRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PhysicalUnitRepository physicalUnitRepository;

    @Override
    @Transactional
    public CameraDTO createCamera(UUID currentUserId, CameraDTO cameraDTO) {
        if (cameraRepository.existsByModelName(cameraDTO.getModelName())) {
            throw new ApiException("Camera with model name '" + cameraDTO.getModelName() + "' already exists.");
        }

        Camera camera = modelMapper.map(cameraDTO, Camera.class);
        camera.setCameraId(null);
        Camera savedCamera = cameraRepository.save(camera);

        return modelMapper.map(savedCamera, CameraDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public CameraDTO getCamera(UUID cameraId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", "cameraId", cameraId));

        return modelMapper.map(camera, CameraDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CameraDTO> getCameras(String search, boolean includeInactive,
                                               Integer pageNumber, Integer pageSize,
                                               String sortBy, String sortOrder) {
        Pageable pageable = paginationHelper.buildPageable(pageNumber, pageSize, sortBy, sortOrder,
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD);

        boolean hasSearch = search != null && !search.isBlank();

        Page<Camera> pageCameras;
        if (includeInactive) {
            pageCameras = hasSearch
                    ? cameraRepository.searchByBrandOrModelName(search, pageable)
                    : cameraRepository.findAll(pageable);
        } else {
            pageCameras = hasSearch
                    ? cameraRepository.searchActiveByBrandOrModelName(search, pageable)
                    : cameraRepository.findAllByIsActiveTrue(pageable);
        }

        return PagedResponse.from(pageCameras, camera -> modelMapper.map(camera, CameraDTO.class));
    }

    @Override
    @Transactional
    public CameraDTO updateCamera(UUID currentUserId, UUID cameraId, CameraDTO cameraDTO) {
        Camera cameraFromDb = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", "cameraId", cameraId));

        if (!cameraFromDb.getModelName().equalsIgnoreCase(cameraDTO.getModelName())
                && cameraRepository.existsByModelName(cameraDTO.getModelName())) {
            throw new ApiException("Camera with model name '" + cameraDTO.getModelName() + "' already exists.");
        }

        cameraFromDb.setBrand(cameraDTO.getBrand());
        cameraFromDb.setModelName(cameraDTO.getModelName());
        cameraFromDb.setCategory(cameraDTO.getCategory());
        cameraFromDb.setSensorFormat(cameraDTO.getSensorFormat());
        cameraFromDb.setLensMount(cameraDTO.getLensMount());
        cameraFromDb.setActive(cameraDTO.isActive());
        cameraFromDb.setVideoCapable(cameraDTO.isVideoCapable());
        cameraFromDb.setPhotoCapable(cameraDTO.isPhotoCapable());
        cameraFromDb.setResolution(cameraDTO.getResolution());
        cameraFromDb.setMaxIso(cameraDTO.getMaxIso());
        cameraFromDb.setMaxFrameRate4k(cameraDTO.getMaxFrameRate4k());
        cameraFromDb.setMaxFrameRate1080p(cameraDTO.getMaxFrameRate1080p());
        cameraFromDb.setDescription(cameraDTO.getDescription());

        Camera savedCamera = cameraRepository.save(cameraFromDb);

        return modelMapper.map(savedCamera, CameraDTO.class);
    }

    @Override
    @Transactional
    public void deleteCamera(UUID cameraId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", "cameraId", cameraId));

        Optional<InventoryItem> inventoryOpt = inventoryItemRepository.findByCameraCameraId(cameraId);

        if (inventoryOpt.isPresent()) {
            InventoryItem inventory = inventoryOpt.get();
            long unitCount = physicalUnitRepository
                    .countByInventoryItemInventoryItemId(inventory.getInventoryItemId());

            if (unitCount > 0) {
                throw new ResourceConflictException(
                        "Cannot delete camera '" + camera.getBrand() + " " + camera.getModelName()
                                + "': " + unitCount + " physical unit(s) still on record. "
                                + "Retire or remove all units first, or deactivate the camera instead.");
            }

            inventoryItemRepository.delete(inventory);
        }

        cameraRepository.delete(camera);
    }

    @Override
    @Transactional
    public CameraDTO deactivateCamera(UUID cameraId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", "cameraId", cameraId));

        camera.setActive(false);
        Camera saved = cameraRepository.save(camera);
        return modelMapper.map(saved, CameraDTO.class);
    }
}
