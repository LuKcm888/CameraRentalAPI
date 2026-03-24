package com.camerarental.backend.service;

import com.camerarental.backend.exceptions.ApiException;
import com.camerarental.backend.exceptions.ResourceConflictException;
import com.camerarental.backend.exceptions.ResourceNotFoundException;
import com.camerarental.backend.model.entity.Camera;
import com.camerarental.backend.model.entity.InventoryItem;
import com.camerarental.backend.model.entity.enums.CameraCategory;
import com.camerarental.backend.model.entity.enums.SensorFormat;
import com.camerarental.backend.payload.CameraDTO;
import com.camerarental.backend.payload.base.PagedResponse;
import com.camerarental.backend.repository.CameraRepository;
import com.camerarental.backend.repository.InventoryItemRepository;
import com.camerarental.backend.repository.PhysicalUnitRepository;
import com.camerarental.backend.util.PaginationHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CameraServiceImplTest {

    @Mock private ModelMapper modelMapper;
    @Mock private PaginationHelper paginationHelper;
    @Mock private CameraRepository cameraRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private PhysicalUnitRepository physicalUnitRepository;

    @InjectMocks
    private CameraServiceImpl service;

    private static final UUID CAMERA_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private Camera sampleCamera() {
        Camera c = new Camera();
        c.setCameraId(CAMERA_ID);
        c.setBrand("Sony");
        c.setModelName("A7 IV");
        c.setCategory(CameraCategory.MIRRORLESS);
        c.setSensorFormat(SensorFormat.FULL_FRAME);
        c.setActive(true);
        return c;
    }

    private CameraDTO sampleDto() {
        CameraDTO dto = new CameraDTO();
        dto.setCameraId(CAMERA_ID);
        dto.setBrand("Sony");
        dto.setModelName("A7 IV");
        dto.setCategory(CameraCategory.MIRRORLESS);
        dto.setSensorFormat(SensorFormat.FULL_FRAME);
        dto.setActive(true);
        return dto;
    }

    // =========================================================================
    // createCamera
    // =========================================================================

    @Nested
    @DisplayName("createCamera")
    class CreateTests {

        @Test
        @DisplayName("creates camera when model name is unique")
        void create_uniqueName_succeeds() {
            CameraDTO inputDto = sampleDto();
            Camera mapped = sampleCamera();
            Camera saved = sampleCamera();
            CameraDTO outputDto = sampleDto();

            given(cameraRepository.existsByModelName("A7 IV")).willReturn(false);
            given(modelMapper.map(inputDto, Camera.class)).willReturn(mapped);
            given(cameraRepository.save(mapped)).willReturn(saved);
            given(modelMapper.map(saved, CameraDTO.class)).willReturn(outputDto);

            CameraDTO result = service.createCamera(USER_ID, inputDto);

            assertThat(result.getBrand()).isEqualTo("Sony");
            verify(cameraRepository).save(mapped);
        }

        @Test
        @DisplayName("throws ApiException when model name already exists")
        void create_duplicateName_throws() {
            CameraDTO dto = sampleDto();
            given(cameraRepository.existsByModelName("A7 IV")).willReturn(true);

            assertThatThrownBy(() -> service.createCamera(USER_ID, dto))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("already exists");

            verify(cameraRepository, never()).save(any());
        }
    }

    // =========================================================================
    // getCamera
    // =========================================================================

    @Nested
    @DisplayName("getCamera")
    class GetByIdTests {

        @Test
        @DisplayName("returns DTO when camera exists")
        void getCamera_found() {
            Camera camera = sampleCamera();
            CameraDTO dto = sampleDto();

            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(camera));
            given(modelMapper.map(camera, CameraDTO.class)).willReturn(dto);

            CameraDTO result = service.getCamera(CAMERA_ID);

            assertThat(result.getCameraId()).isEqualTo(CAMERA_ID);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when camera missing")
        void getCamera_notFound() {
            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCamera(CAMERA_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // getCameras — search/active matrix
    // =========================================================================

    @Nested
    @DisplayName("getCameras")
    class GetAllTests {

        private final Pageable pageable = PageRequest.of(0, 10);

        private void stubPagination() {
            given(paginationHelper.buildPageable(any(), any(), any(), any(), any(), any()))
                    .willReturn(pageable);
        }

        @Test
        @DisplayName("active-only, no search → findAllByIsActiveTrue")
        void getCameras_activeOnly_noSearch() {
            Page<Camera> page = new PageImpl<>(List.of(sampleCamera()));
            stubPagination();
            given(cameraRepository.findAllByIsActiveTrue(pageable)).willReturn(page);
            given(modelMapper.map(any(Camera.class), eq(CameraDTO.class))).willReturn(sampleDto());

            PagedResponse<CameraDTO> result = service.getCameras(null, false, 0, 10, "cameraId", "asc");

            assertThat(result.getContent()).hasSize(1);
            verify(cameraRepository).findAllByIsActiveTrue(pageable);
            verify(cameraRepository, never()).findAll(pageable);
        }

        @Test
        @DisplayName("active-only, with search → searchActiveByBrandOrModelName")
        void getCameras_activeOnly_withSearch() {
            Page<Camera> page = new PageImpl<>(List.of(sampleCamera()));
            stubPagination();
            given(cameraRepository.searchActiveByBrandOrModelName("Sony", pageable)).willReturn(page);
            given(modelMapper.map(any(Camera.class), eq(CameraDTO.class))).willReturn(sampleDto());

            PagedResponse<CameraDTO> result = service.getCameras("Sony", false, 0, 10, "cameraId", "asc");

            assertThat(result.getContent()).hasSize(1);
            verify(cameraRepository).searchActiveByBrandOrModelName("Sony", pageable);
        }

        @Test
        @DisplayName("includeInactive, no search → findAll")
        void getCameras_includeInactive_noSearch() {
            Page<Camera> page = new PageImpl<>(List.of(sampleCamera()));
            stubPagination();
            given(cameraRepository.findAll(pageable)).willReturn(page);
            given(modelMapper.map(any(Camera.class), eq(CameraDTO.class))).willReturn(sampleDto());

            service.getCameras(null, true, 0, 10, "cameraId", "asc");

            verify(cameraRepository).findAll(pageable);
        }

        @Test
        @DisplayName("includeInactive, with search → searchByBrandOrModelName")
        void getCameras_includeInactive_withSearch() {
            Page<Camera> page = new PageImpl<>(List.of(sampleCamera()));
            stubPagination();
            given(cameraRepository.searchByBrandOrModelName("Canon", pageable)).willReturn(page);
            given(modelMapper.map(any(Camera.class), eq(CameraDTO.class))).willReturn(sampleDto());

            service.getCameras("Canon", true, 0, 10, "cameraId", "asc");

            verify(cameraRepository).searchByBrandOrModelName("Canon", pageable);
        }

        @Test
        @DisplayName("blank search string treated as no search")
        void getCameras_blankSearch_treatedAsNull() {
            Page<Camera> page = new PageImpl<>(List.of());
            stubPagination();
            given(cameraRepository.findAllByIsActiveTrue(pageable)).willReturn(page);

            service.getCameras("   ", false, 0, 10, "cameraId", "asc");

            verify(cameraRepository).findAllByIsActiveTrue(pageable);
        }
    }

    // =========================================================================
    // updateCamera
    // =========================================================================

    @Nested
    @DisplayName("updateCamera")
    class UpdateTests {

        @Test
        @DisplayName("updates camera when model name unchanged")
        void update_sameModelName_succeeds() {
            Camera existing = sampleCamera();
            CameraDTO dto = sampleDto();
            Camera saved = sampleCamera();
            CameraDTO outputDto = sampleDto();

            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(existing));
            given(cameraRepository.save(existing)).willReturn(saved);
            given(modelMapper.map(saved, CameraDTO.class)).willReturn(outputDto);

            assertThat(service.updateCamera(USER_ID, CAMERA_ID, dto)).isNotNull();
            verify(cameraRepository, never()).existsByModelName(anyString());
        }

        @Test
        @DisplayName("updates camera when model name changes to a unique value")
        void update_newUniqueName_succeeds() {
            Camera existing = sampleCamera();
            CameraDTO dto = sampleDto();
            dto.setModelName("A7 V");
            Camera saved = sampleCamera();
            CameraDTO outputDto = sampleDto();

            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(existing));
            given(cameraRepository.existsByModelName("A7 V")).willReturn(false);
            given(cameraRepository.save(existing)).willReturn(saved);
            given(modelMapper.map(saved, CameraDTO.class)).willReturn(outputDto);

            CameraDTO result = service.updateCamera(USER_ID, CAMERA_ID, dto);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("throws ApiException when new model name collides")
        void update_duplicateName_throws() {
            Camera existing = sampleCamera();
            CameraDTO dto = sampleDto();
            dto.setModelName("R5");

            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(existing));
            given(cameraRepository.existsByModelName("R5")).willReturn(true);

            assertThatThrownBy(() -> service.updateCamera(USER_ID, CAMERA_ID, dto))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when camera missing")
        void update_notFound() {
            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCamera(USER_ID, CAMERA_ID, sampleDto()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // deleteCamera — business rule enforcement
    // =========================================================================

    @Nested
    @DisplayName("deleteCamera")
    class DeleteTests {

        @Test
        @DisplayName("deletes camera with no inventory record")
        void delete_noInventory_succeeds() {
            Camera camera = sampleCamera();
            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(camera));
            given(inventoryItemRepository.findByCameraCameraId(CAMERA_ID)).willReturn(Optional.empty());

            service.deleteCamera(CAMERA_ID);

            verify(cameraRepository).delete(camera);
            verify(inventoryItemRepository, never()).delete(any());
        }

        @Test
        @DisplayName("cascade-deletes empty inventory then camera")
        void delete_emptyInventory_cascadeDeletes() {
            Camera camera = sampleCamera();
            UUID invId = UUID.randomUUID();
            InventoryItem inventory = new InventoryItem();
            inventory.setInventoryItemId(invId);

            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(camera));
            given(inventoryItemRepository.findByCameraCameraId(CAMERA_ID)).willReturn(Optional.of(inventory));
            given(physicalUnitRepository.countByInventoryItemInventoryItemId(invId)).willReturn(0L);

            service.deleteCamera(CAMERA_ID);

            verify(inventoryItemRepository).delete(inventory);
            verify(cameraRepository).delete(camera);
        }

        @Test
        @DisplayName("throws ResourceConflictException when physical units exist")
        void delete_withUnits_throwsConflict() {
            Camera camera = sampleCamera();
            UUID invId = UUID.randomUUID();
            InventoryItem inventory = new InventoryItem();
            inventory.setInventoryItemId(invId);

            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(camera));
            given(inventoryItemRepository.findByCameraCameraId(CAMERA_ID)).willReturn(Optional.of(inventory));
            given(physicalUnitRepository.countByInventoryItemInventoryItemId(invId)).willReturn(3L);

            assertThatThrownBy(() -> service.deleteCamera(CAMERA_ID))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("3 physical unit(s) still on record");

            verify(cameraRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when camera missing")
        void delete_notFound() {
            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCamera(CAMERA_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // deactivateCamera
    // =========================================================================

    @Nested
    @DisplayName("deactivateCamera")
    class DeactivateTests {

        @Test
        @DisplayName("sets active to false and returns updated DTO")
        void deactivate_setsInactive() {
            Camera camera = sampleCamera();
            Camera saved = sampleCamera();
            saved.setActive(false);
            CameraDTO dto = sampleDto();
            dto.setActive(false);

            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(camera));
            given(cameraRepository.save(camera)).willReturn(saved);
            given(modelMapper.map(saved, CameraDTO.class)).willReturn(dto);

            CameraDTO result = service.deactivateCamera(CAMERA_ID);

            assertThat(result.isActive()).isFalse();
            verify(cameraRepository).save(camera);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when camera missing")
        void deactivate_notFound() {
            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivateCamera(CAMERA_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
