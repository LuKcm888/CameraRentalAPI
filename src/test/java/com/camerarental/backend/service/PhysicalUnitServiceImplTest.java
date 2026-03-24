package com.camerarental.backend.service;

import com.camerarental.backend.exceptions.ApiException;
import com.camerarental.backend.exceptions.ResourceNotFoundException;
import com.camerarental.backend.model.entity.InventoryItem;
import com.camerarental.backend.model.entity.PhysicalUnit;
import com.camerarental.backend.model.entity.enums.UnitCondition;
import com.camerarental.backend.model.entity.enums.UnitStatus;
import com.camerarental.backend.payload.PhysicalUnitDTO;
import com.camerarental.backend.payload.base.PagedResponse;
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

import java.time.LocalDate;
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
class PhysicalUnitServiceImplTest {

    @Mock private PhysicalUnitRepository physicalUnitRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private PaginationHelper paginationHelper;

    @InjectMocks
    private PhysicalUnitServiceImpl service;

    private static final UUID INVENTORY_ID = UUID.randomUUID();
    private static final UUID UNIT_ID = UUID.randomUUID();

    private InventoryItem sampleInventoryItem() {
        InventoryItem item = new InventoryItem();
        item.setInventoryItemId(INVENTORY_ID);
        return item;
    }

    private PhysicalUnit sampleUnit() {
        PhysicalUnit unit = new PhysicalUnit();
        unit.setPhysicalUnitId(UNIT_ID);
        unit.setInventoryItem(sampleInventoryItem());
        unit.setSerialNumber("SN-001");
        unit.setCondition(UnitCondition.NEW);
        unit.setStatus(UnitStatus.AVAILABLE);
        unit.setAcquiredDate(LocalDate.of(2025, 6, 1));
        return unit;
    }

    private PhysicalUnitDTO sampleDto() {
        PhysicalUnitDTO dto = new PhysicalUnitDTO();
        dto.setInventoryItemId(INVENTORY_ID);
        dto.setSerialNumber("SN-001");
        dto.setCondition(UnitCondition.NEW);
        dto.setStatus(UnitStatus.AVAILABLE);
        dto.setAcquiredDate(LocalDate.of(2025, 6, 1));
        return dto;
    }

    // =========================================================================
    // create
    // =========================================================================

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("creates unit for valid inventory item")
        void create_success() {
            PhysicalUnitDTO dto = sampleDto();
            InventoryItem parent = sampleInventoryItem();
            PhysicalUnit saved = sampleUnit();

            given(inventoryItemRepository.findById(INVENTORY_ID)).willReturn(Optional.of(parent));
            given(physicalUnitRepository.existsBySerialNumber("SN-001")).willReturn(false);
            given(physicalUnitRepository.save(any(PhysicalUnit.class))).willReturn(saved);

            PhysicalUnitDTO result = service.create(dto);

            assertThat(result.getSerialNumber()).isEqualTo("SN-001");
            assertThat(result.getInventoryItemId()).isEqualTo(INVENTORY_ID);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when inventory item missing")
        void create_parentNotFound() {
            PhysicalUnitDTO dto = sampleDto();
            given(inventoryItemRepository.findById(INVENTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(physicalUnitRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ApiException when serial number already exists")
        void create_duplicateSerial() {
            PhysicalUnitDTO dto = sampleDto();
            given(inventoryItemRepository.findById(INVENTORY_ID)).willReturn(Optional.of(sampleInventoryItem()));
            given(physicalUnitRepository.existsBySerialNumber("SN-001")).willReturn(true);

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("serial number");

            verify(physicalUnitRepository, never()).save(any());
        }
    }

    // =========================================================================
    // getByInventoryItem
    // =========================================================================

    @Nested
    @DisplayName("getByInventoryItem")
    class GetByInventoryItemTests {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("returns paginated units")
        void getByInventoryItem_success() {
            Page<PhysicalUnit> page = new PageImpl<>(List.of(sampleUnit()));

            given(inventoryItemRepository.existsById(INVENTORY_ID)).willReturn(true);
            given(paginationHelper.buildPageable(any(), any(), any(), any(), any(), any()))
                    .willReturn(pageable);
            given(physicalUnitRepository.findByInventoryItemInventoryItemId(INVENTORY_ID, pageable))
                    .willReturn(page);

            PagedResponse<PhysicalUnitDTO> result = service.getByInventoryItem(
                    INVENTORY_ID, 0, 10, "physicalUnitId", "asc");

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when inventory item missing")
        void getByInventoryItem_parentNotFound() {
            given(inventoryItemRepository.existsById(INVENTORY_ID)).willReturn(false);

            assertThatThrownBy(() -> service.getByInventoryItem(
                    INVENTORY_ID, 0, 10, "physicalUnitId", "asc"))
                    .isInstanceOf(ResourceNotFoundException.class);
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
            given(physicalUnitRepository.findById(UNIT_ID)).willReturn(Optional.of(sampleUnit()));

            PhysicalUnitDTO result = service.getById(UNIT_ID);

            assertThat(result.getPhysicalUnitId()).isEqualTo(UNIT_ID);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when missing")
        void getById_notFound() {
            given(physicalUnitRepository.findById(UNIT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(UNIT_ID))
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
        @DisplayName("updates fields when serial number unchanged")
        void update_sameSerial_succeeds() {
            PhysicalUnit existing = sampleUnit();
            PhysicalUnitDTO dto = sampleDto();
            dto.setCondition(UnitCondition.GOOD);

            given(physicalUnitRepository.findById(UNIT_ID)).willReturn(Optional.of(existing));
            given(physicalUnitRepository.save(existing)).willReturn(existing);

            service.update(UNIT_ID, dto);

            assertThat(existing.getCondition()).isEqualTo(UnitCondition.GOOD);
            verify(physicalUnitRepository, never()).existsBySerialNumber(any());
        }

        @Test
        @DisplayName("updates serial number when new value is unique")
        void update_newUniqueSerial_succeeds() {
            PhysicalUnit existing = sampleUnit();
            PhysicalUnitDTO dto = sampleDto();
            dto.setSerialNumber("SN-002");

            given(physicalUnitRepository.findById(UNIT_ID)).willReturn(Optional.of(existing));
            given(physicalUnitRepository.existsBySerialNumber("SN-002")).willReturn(false);
            given(physicalUnitRepository.save(existing)).willReturn(existing);

            service.update(UNIT_ID, dto);

            assertThat(existing.getSerialNumber()).isEqualTo("SN-002");
        }

        @Test
        @DisplayName("throws ApiException when new serial number collides")
        void update_duplicateSerial_throws() {
            PhysicalUnit existing = sampleUnit();
            PhysicalUnitDTO dto = sampleDto();
            dto.setSerialNumber("SN-TAKEN");

            given(physicalUnitRepository.findById(UNIT_ID)).willReturn(Optional.of(existing));
            given(physicalUnitRepository.existsBySerialNumber("SN-TAKEN")).willReturn(true);

            assertThatThrownBy(() -> service.update(UNIT_ID, dto))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("serial number");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when missing")
        void update_notFound() {
            given(physicalUnitRepository.findById(UNIT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(UNIT_ID, sampleDto()))
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
            PhysicalUnit unit = sampleUnit();
            given(physicalUnitRepository.findById(UNIT_ID)).willReturn(Optional.of(unit));

            service.delete(UNIT_ID);

            verify(physicalUnitRepository).delete(unit);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when missing")
        void delete_notFound() {
            given(physicalUnitRepository.findById(UNIT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(UNIT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
