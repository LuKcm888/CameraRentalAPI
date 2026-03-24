package com.camerarental.backend.controller;

import com.camerarental.backend.config.AbstractPostgresIT;
import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.config.PostgresIntegrationTest;
import com.camerarental.backend.model.entity.Camera;
import com.camerarental.backend.model.entity.InventoryItem;
import com.camerarental.backend.model.entity.PhysicalUnit;
import com.camerarental.backend.model.entity.User;
import com.camerarental.backend.model.entity.enums.UnitCondition;
import com.camerarental.backend.model.entity.enums.UnitStatus;
import com.camerarental.backend.payload.PhysicalUnitDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PostgresIntegrationTest
class PhysicalUnitControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    private User admin;
    private User customer;
    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        admin = createAdmin();
        customer = createCustomer();
        Camera camera = createCamera("Canon", "R5");
        inventoryItem = createInventoryItem(camera, "200.00", "3500.00");
    }

    private PhysicalUnitDTO validDto() {
        PhysicalUnitDTO dto = new PhysicalUnitDTO();
        dto.setInventoryItemId(inventoryItem.getInventoryItemId());
        dto.setSerialNumber("CANON-R5-001");
        dto.setCondition(UnitCondition.NEW);
        dto.setStatus(UnitStatus.AVAILABLE);
        dto.setAcquiredDate(LocalDate.of(2025, 6, 1));
        return dto;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/units
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST " + ApiPaths.PHYSICAL_UNITS)
    class CreateTests {

        @Test
        @DisplayName("admin creates a physical unit and gets 201")
        void create_asAdmin_returns201() throws Exception {
            mockMvc.perform(post(ApiPaths.PHYSICAL_UNITS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validDto())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.physicalUnitId").isString())
                    .andExpect(jsonPath("$.inventoryItemId").value(inventoryItem.getInventoryItemId().toString()))
                    .andExpect(jsonPath("$.serialNumber").value("CANON-R5-001"))
                    .andExpect(jsonPath("$.condition").value("NEW"))
                    .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }

        @Test
        @DisplayName("customer gets 403 creating a unit")
        void create_asCustomer_returns403() throws Exception {
            mockMvc.perform(post(ApiPaths.PHYSICAL_UNITS)
                            .with(authenticated(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validDto())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("missing serial number returns 400")
        void create_missingSerial_returns400() throws Exception {
            PhysicalUnitDTO dto = validDto();
            dto.setSerialNumber(null);

            mockMvc.perform(post(ApiPaths.PHYSICAL_UNITS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/units/inventory/{inventoryItemId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET " + ApiPaths.PHYSICAL_UNITS + "/inventory/{inventoryItemId}")
    class GetByInventoryItemTests {

        @Test
        @DisplayName("returns paginated units for an inventory item")
        void getByInventoryItem_returnsList() throws Exception {
            createPhysicalUnit(inventoryItem, "SN-001");
            createPhysicalUnit(inventoryItem, "SN-002");
            createPhysicalUnit(inventoryItem, "SN-003");

            mockMvc.perform(get(ApiPaths.PHYSICAL_UNITS + "/inventory/" + inventoryItem.getInventoryItemId())
                            .with(authenticated(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("returns empty page for item with no units")
        void getByInventoryItem_noUnits() throws Exception {
            mockMvc.perform(get(ApiPaths.PHYSICAL_UNITS + "/inventory/" + inventoryItem.getInventoryItemId())
                            .with(authenticated(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/units/{physicalUnitId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET " + ApiPaths.PHYSICAL_UNITS + "/{physicalUnitId}")
    class GetByIdTests {

        @Test
        @DisplayName("returns unit by ID")
        void getById_found() throws Exception {
            PhysicalUnit unit = createPhysicalUnit(inventoryItem, "SN-001");

            mockMvc.perform(get(ApiPaths.PHYSICAL_UNITS + "/" + unit.getPhysicalUnitId())
                            .with(authenticated(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.serialNumber").value("SN-001"));
        }

        @Test
        @DisplayName("returns 404 for non-existent unit")
        void getById_notFound() throws Exception {
            mockMvc.perform(get(ApiPaths.PHYSICAL_UNITS + "/00000000-0000-0000-0000-000000000001")
                            .with(authenticated(customer)))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/units/{physicalUnitId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT " + ApiPaths.PHYSICAL_UNITS + "/{physicalUnitId}")
    class UpdateTests {

        @Test
        @DisplayName("admin can update unit condition and status")
        void update_asAdmin_returnsUpdated() throws Exception {
            PhysicalUnit unit = createPhysicalUnit(inventoryItem, "SN-001");

            PhysicalUnitDTO updateDto = new PhysicalUnitDTO();
            updateDto.setInventoryItemId(inventoryItem.getInventoryItemId());
            updateDto.setSerialNumber("SN-001");
            updateDto.setCondition(UnitCondition.GOOD);
            updateDto.setStatus(UnitStatus.RENTED);
            updateDto.setNotes("Rented to customer A");

            mockMvc.perform(put(ApiPaths.PHYSICAL_UNITS + "/" + unit.getPhysicalUnitId())
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.condition").value("GOOD"))
                    .andExpect(jsonPath("$.status").value("RENTED"))
                    .andExpect(jsonPath("$.notes").value("Rented to customer A"));
        }

        @Test
        @DisplayName("customer gets 403 when updating a unit")
        void update_asCustomer_returns403() throws Exception {
            PhysicalUnit unit = createPhysicalUnit(inventoryItem, "SN-001");

            mockMvc.perform(put(ApiPaths.PHYSICAL_UNITS + "/" + unit.getPhysicalUnitId())
                            .with(authenticated(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validDto())))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/units/{physicalUnitId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE " + ApiPaths.PHYSICAL_UNITS + "/{physicalUnitId}")
    class DeleteTests {

        @Test
        @DisplayName("admin can delete a unit and gets 204")
        void delete_asAdmin_returns204() throws Exception {
            PhysicalUnit unit = createPhysicalUnit(inventoryItem, "SN-001");

            mockMvc.perform(delete(ApiPaths.PHYSICAL_UNITS + "/" + unit.getPhysicalUnitId())
                            .with(authenticated(admin)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(ApiPaths.PHYSICAL_UNITS + "/" + unit.getPhysicalUnitId())
                            .with(authenticated(admin)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("customer gets 403 when deleting")
        void delete_asCustomer_returns403() throws Exception {
            PhysicalUnit unit = createPhysicalUnit(inventoryItem, "SN-001");

            mockMvc.perform(delete(ApiPaths.PHYSICAL_UNITS + "/" + unit.getPhysicalUnitId())
                            .with(authenticated(customer)))
                    .andExpect(status().isForbidden());
        }
    }
}
