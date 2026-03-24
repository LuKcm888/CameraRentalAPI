package com.camerarental.backend.controller;

import com.camerarental.backend.config.AbstractPostgresIT;
import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.config.PostgresIntegrationTest;
import com.camerarental.backend.model.entity.Camera;
import com.camerarental.backend.model.entity.InventoryItem;
import com.camerarental.backend.model.entity.User;
import com.camerarental.backend.payload.InventoryItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PostgresIntegrationTest
class InventoryItemControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    private User admin;
    private User customer;
    private Camera sony;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        admin = createAdmin();
        customer = createCustomer();
        sony = createCamera("Sony", "A7 IV");
    }

    private InventoryItemDTO validDto(Camera camera) {
        InventoryItemDTO dto = new InventoryItemDTO();
        dto.setCameraId(camera.getCameraId());
        dto.setDailyRentalPrice(new BigDecimal("150.00"));
        dto.setReplacementValue(new BigDecimal("2500.00"));
        return dto;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/inventory
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST " + ApiPaths.INVENTORY)
    class CreateTests {

        @Test
        @DisplayName("admin creates inventory item and gets 201 with computed counts")
        void create_asAdmin_returns201() throws Exception {
            mockMvc.perform(post(ApiPaths.INVENTORY)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validDto(sony))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.inventoryItemId").isString())
                    .andExpect(jsonPath("$.cameraId").value(sony.getCameraId().toString()))
                    .andExpect(jsonPath("$.cameraBrand").value("Sony"))
                    .andExpect(jsonPath("$.cameraModelName").value("A7 IV"))
                    .andExpect(jsonPath("$.dailyRentalPrice").value(150.00))
                    .andExpect(jsonPath("$.totalUnits").value(0))
                    .andExpect(jsonPath("$.availableUnits").value(0));
        }

        @Test
        @DisplayName("customer gets 403 creating inventory")
        void create_asCustomer_returns403() throws Exception {
            mockMvc.perform(post(ApiPaths.INVENTORY)
                            .with(authenticated(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validDto(sony))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("missing required fields returns 400")
        void create_missingFields_returns400() throws Exception {
            mockMvc.perform(post(ApiPaths.INVENTORY)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventory
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET " + ApiPaths.INVENTORY)
    class GetAllTests {

        @Test
        @DisplayName("returns paginated list with unit counts")
        void getAll_withUnits_includesCounts() throws Exception {
            InventoryItem item = createInventoryItem(sony, "150.00", "2500.00");
            createPhysicalUnit(item, "SN-001");
            createPhysicalUnit(item, "SN-002");

            mockMvc.perform(get(ApiPaths.INVENTORY)
                            .with(authenticated(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].totalUnits").value(2))
                    .andExpect(jsonPath("$.content[0].availableUnits").value(2))
                    .andExpect(jsonPath("$.content[0].cameraBrand").value("Sony"));
        }

        @Test
        @DisplayName("returns empty page when no inventory exists")
        void getAll_empty() throws Exception {
            mockMvc.perform(get(ApiPaths.INVENTORY)
                            .with(authenticated(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/inventory/{inventoryItemId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET " + ApiPaths.INVENTORY + "/{inventoryItemId}")
    class GetByIdTests {

        @Test
        @DisplayName("returns inventory item with counts")
        void getById_found() throws Exception {
            InventoryItem item = createInventoryItem(sony, "150.00", "2500.00");
            createPhysicalUnit(item, "SN-001");

            mockMvc.perform(get(ApiPaths.INVENTORY + "/" + item.getInventoryItemId())
                            .with(authenticated(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.inventoryItemId").value(item.getInventoryItemId().toString()))
                    .andExpect(jsonPath("$.totalUnits").value(1))
                    .andExpect(jsonPath("$.availableUnits").value(1));
        }

        @Test
        @DisplayName("returns 404 for non-existent item")
        void getById_notFound() throws Exception {
            mockMvc.perform(get(ApiPaths.INVENTORY + "/00000000-0000-0000-0000-000000000001")
                            .with(authenticated(customer)))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/inventory/{inventoryItemId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT " + ApiPaths.INVENTORY + "/{inventoryItemId}")
    class UpdateTests {

        @Test
        @DisplayName("admin can update pricing")
        void update_asAdmin_returnsUpdated() throws Exception {
            InventoryItem item = createInventoryItem(sony, "150.00", "2500.00");

            InventoryItemDTO updateDto = new InventoryItemDTO();
            updateDto.setCameraId(sony.getCameraId());
            updateDto.setDailyRentalPrice(new BigDecimal("200.00"));
            updateDto.setReplacementValue(new BigDecimal("3000.00"));

            mockMvc.perform(put(ApiPaths.INVENTORY + "/" + item.getInventoryItemId())
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailyRentalPrice").value(200.00))
                    .andExpect(jsonPath("$.replacementValue").value(3000.00));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/inventory/{inventoryItemId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE " + ApiPaths.INVENTORY + "/{inventoryItemId}")
    class DeleteTests {

        @Test
        @DisplayName("admin can delete inventory item and gets 204")
        void delete_asAdmin_returns204() throws Exception {
            InventoryItem item = createInventoryItem(sony, "150.00", "2500.00");
            createPhysicalUnit(item, "SN-001");

            mockMvc.perform(delete(ApiPaths.INVENTORY + "/" + item.getInventoryItemId())
                            .with(authenticated(admin)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(ApiPaths.INVENTORY + "/" + item.getInventoryItemId())
                            .with(authenticated(admin)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("customer gets 403 when deleting")
        void delete_asCustomer_returns403() throws Exception {
            InventoryItem item = createInventoryItem(sony, "150.00", "2500.00");

            mockMvc.perform(delete(ApiPaths.INVENTORY + "/" + item.getInventoryItemId())
                            .with(authenticated(customer)))
                    .andExpect(status().isForbidden());
        }
    }
}
