package com.camerarental.backend.service;

import com.camerarental.backend.exceptions.ApiException;
import com.camerarental.backend.exceptions.ResourceNotFoundException;
import com.camerarental.backend.model.entity.Camera;
import com.camerarental.backend.model.entity.InventoryItem;
import com.camerarental.backend.model.entity.enums.CameraCategory;
import com.camerarental.backend.model.entity.enums.SensorFormat;
import com.camerarental.backend.model.entity.enums.UnitStatus;
import com.camerarental.backend.payload.InventoryItemDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceImplTest {

    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private CameraRepository cameraRepository;
    @Mock private PhysicalUnitRepository physicalUnitRepository;
    @Mock private PaginationHelper paginationHelper;

    @InjectMocks
    private InventoryItemServiceImpl service;

    private static final UUID CAMERA_ID = UUID.randomUUID();
    private static final UUID INVENTORY_ID = UUID.randomUUID();

    private Camera sampleCamera() {
        Camera c = new Camera();
        c.setCameraId(CAMERA_ID);
        c.setBrand("Canon");
        c.setModelName("R5");
        c.setCategory(CameraCategory.MIRRORLESS);
        c.setSensorFormat(SensorFormat.FULL_FRAME);
        return c;
    }

    private InventoryItem sampleInventoryItem() {
        InventoryItem item = new InventoryItem();
        item.setInventoryItemId(INVENTORY_ID);
        item.setCamera(sampleCamera());
        item.setDailyRentalPrice(new BigDecimal("75.00"));
        item.setReplacementValue(new BigDecimal("3500.00"));
        return item;
    }

    private InventoryItemDTO sampleDto() {
        InventoryItemDTO dto = new InventoryItemDTO();
        dto.setCameraId(CAMERA_ID);
        dto.setDailyRentalPrice(new BigDecimal("75.00"));
        dto.setReplacementValue(new BigDecimal("3500.00"));
        return dto;
    }

    // =========================================================================
    // create
    // =========================================================================

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("creates inventory item for valid camera")
        void create_success() {
            InventoryItemDTO dto = sampleDto();
            Camera camera = sampleCamera();
            InventoryItem saved = sampleInventoryItem();

            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(camera));
            given(inventoryItemRepository.existsByCameraCameraId(CAMERA_ID)).willReturn(false);
            given(inventoryItemRepository.save(any(InventoryItem.class))).willReturn(saved);
            given(physicalUnitRepository.countByInventoryItemInventoryItemId(INVENTORY_ID)).willReturn(0L);
            given(physicalUnitRepository.countByInventoryItemInventoryItemIdAndStatus(INVENTORY_ID, UnitStatus.AVAILABLE))
                    .willReturn(0L);

            InventoryItemDTO result = service.create(dto);

            assertThat(result.getCameraBrand()).isEqualTo("Canon");
            assertThat(result.getTotalUnits()).isZero();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when camera missing")
        void create_cameraNotFound() {
            InventoryItemDTO dto = sampleDto();
            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(inventoryItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ApiException when inventory already exists for camera")
        void create_duplicateCamera() {
            InventoryItemDTO dto = sampleDto();
            given(cameraRepository.findById(CAMERA_ID)).willReturn(Optional.of(sampleCamera()));
            given(inventoryItemRepository.existsByCameraCameraId(CAMERA_ID)).willReturn(true);

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("already exists");
        }
    }

    // =========================================================================
    // getAll — batched counts
    // =========================================================================

    @Nested
    @DisplayName("getAll")
    class GetAllTests {

        private final Pageable pageable = PageRequest.of(0, 10);

        private void stubPagination() {
            given(paginationHelper.buildPageable(any(), any(), any(), any(), any(), any()))
                    .willReturn(pageable);
        }

        @Test
        @DisplayName("returns DTOs with batched unit counts")
        void getAll_withCounts() {
            InventoryItem item = sampleInventoryItem();
            Page<InventoryItem> page = new PageImpl<>(List.of(item));

            stubPagination();
            given(inventoryItemRepository.findAll(pageable)).willReturn(page);
            List<Object[]> batchResult = new ArrayList<>();
            batchResult.add(new Object[]{INVENTORY_ID, 5L, 3L});
            given(physicalUnitRepository.batchCountsByInventoryItemIds(any()))
                    .willReturn(batchResult);

            PagedResponse<InventoryItemDTO> result = service.getAll(0, 10, "inventoryItemId", "asc");

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTotalUnits()).isEqualTo(5L);
            assertThat(result.getContent().get(0).getAvailableUnits()).isEqualTo(3L);
        }

        @Test
        @DisplayName("empty page skips batch query and returns zeros")
        void getAll_emptyPage() {
            Page<InventoryItem> page = new PageImpl<>(Collections.emptyList());

            stubPagination();
            given(inventoryItemRepository.findAll(pageable)).willReturn(page);

            PagedResponse<InventoryItemDTO> result = service.getAll(0, 10, "inventoryItemId", "asc");

            assertThat(result.getContent()).isEmpty();
            verify(physicalUnitRepository, never()).batchCountsByInventoryItemIds(any());
        }

        @Test
        @DisplayName("items missing from batch counts default to zero")
        void getAll_missingCountsDefaultToZero() {
            InventoryItem item = sampleInventoryItem();
            Page<InventoryItem> page = new PageImpl<>(List.of(item));

            stubPagination();
            given(inventoryItemRepository.findAll(pageable)).willReturn(page);
            given(physicalUnitRepository.batchCountsByInventoryItemIds(any()))
                    .willReturn(Collections.emptyList());

            PagedResponse<InventoryItemDTO> result = service.getAll(0, 10, "inventoryItemId", "asc");

            assertThat(result.getContent().get(0).getTotalUnits()).isZero();
            assertThat(result.getContent().get(0).getAvailableUnits()).isZero();
        }
    }

    // =========================================================================
    // getById
    // =========================================================================

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("returns DTO when found")
        void getById_found() {
            InventoryItem item = sampleInventoryItem();
            given(inventoryItemRepository.findWithCameraByInventoryItemId(INVENTORY_ID))
                    .willReturn(Optional.of(item));
            given(physicalUnitRepository.countByInventoryItemInventoryItemId(INVENTORY_ID)).willReturn(2L);
            given(physicalUnitRepository.countByInventoryItemInventoryItemIdAndStatus(INVENTORY_ID, UnitStatus.AVAILABLE))
                    .willReturn(1L);

            InventoryItemDTO result = service.getById(INVENTORY_ID);

            assertThat(result.getInventoryItemId()).isEqualTo(INVENTORY_ID);
            assertThat(result.getTotalUnits()).isEqualTo(2L);
            assertThat(result.getAvailableUnits()).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when missing")
        void getById_notFound() {
            given(inventoryItemRepository.findWithCameraByInventoryItemId(INVENTORY_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(INVENTORY_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // update
    // =========================================================================

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("updates pricing fields")
        void update_success() {
            InventoryItem existing = sampleInventoryItem();
            InventoryItemDTO dto = sampleDto();
            dto.setDailyRentalPrice(new BigDecimal("100.00"));

            given(inventoryItemRepository.findWithCameraByInventoryItemId(INVENTORY_ID))
                    .willReturn(Optional.of(existing));
            given(inventoryItemRepository.save(existing)).willReturn(existing);
            given(physicalUnitRepository.countByInventoryItemInventoryItemId(INVENTORY_ID)).willReturn(0L);
            given(physicalUnitRepository.countByInventoryItemInventoryItemIdAndStatus(INVENTORY_ID, UnitStatus.AVAILABLE))
                    .willReturn(0L);

            service.update(INVENTORY_ID, dto);

            assertThat(existing.getDailyRentalPrice()).isEqualTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when missing")
        void update_notFound() {
            given(inventoryItemRepository.findWithCameraByInventoryItemId(INVENTORY_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(INVENTORY_ID, sampleDto()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // delete
    // =========================================================================

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deletes when found")
        void delete_success() {
            InventoryItem item = sampleInventoryItem();
            given(inventoryItemRepository.findById(INVENTORY_ID)).willReturn(Optional.of(item));

            service.delete(INVENTORY_ID);

            verify(inventoryItemRepository).delete(item);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when missing")
        void delete_notFound() {
            given(inventoryItemRepository.findById(INVENTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(INVENTORY_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
