package com.camerarental.backend.controller;

import com.camerarental.backend.config.AbstractPostgresIT;
import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.config.PostgresIntegrationTest;
import com.camerarental.backend.model.entity.Camera;
import com.camerarental.backend.model.entity.User;
import com.camerarental.backend.payload.CameraDTO;
import com.camerarental.backend.model.entity.enums.CameraCategory;
import com.camerarental.backend.model.entity.enums.SensorFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PostgresIntegrationTest
class CameraControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    private User admin;
    private User customer;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        admin = createAdmin();
        customer = createCustomer();
    }

    private CameraDTO validCameraDto() {
        CameraDTO dto = new CameraDTO();
        dto.setBrand("Sony");
        dto.setModelName("A7 IV");
        dto.setCategory(CameraCategory.MIRRORLESS);
        dto.setSensorFormat(SensorFormat.FULL_FRAME);
        dto.setVideoCapable(true);
        dto.setPhotoCapable(true);
        dto.setActive(true);
        return dto;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/cameras
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST " + ApiPaths.CAMERAS)
    class CreateTests {

        @Test
        @DisplayName("admin can create a camera and gets 201")
        void create_asAdmin_returns201() throws Exception {
            mockMvc.perform(post(ApiPaths.CAMERAS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validCameraDto())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cameraId").isString())
                    .andExpect(jsonPath("$.brand").value("Sony"))
                    .andExpect(jsonPath("$.modelName").value("A7 IV"))
                    .andExpect(jsonPath("$.category").value("MIRRORLESS"))
                    .andExpect(jsonPath("$.sensorFormat").value("FULL_FRAME"));
        }

        @Test
        @DisplayName("customer gets 403 when creating a camera")
        void create_asCustomer_returns403() throws Exception {
            mockMvc.perform(post(ApiPaths.CAMERAS)
                            .with(authenticated(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validCameraDto())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("invalid body returns 400 with validation errors")
        void create_invalidBody_returns400() throws Exception {
            CameraDTO dto = new CameraDTO();

            mockMvc.perform(post(ApiPaths.CAMERAS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/cameras
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET " + ApiPaths.CAMERAS)
    class GetAllTests {

        @Test
        @DisplayName("returns paginated list of cameras")
        void getAll_returnsPaginatedList() throws Exception {
            createCamera("Sony", "A7 IV");
            createCamera("Canon", "R5");

            mockMvc.perform(get(ApiPaths.CAMERAS)
                            .with(authenticated(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }

        @Test
        @DisplayName("returns empty page when no cameras exist")
        void getAll_empty_returnsEmptyPage() throws Exception {
            mockMvc.perform(get(ApiPaths.CAMERAS)
                            .with(authenticated(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/cameras/{cameraId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET " + ApiPaths.CAMERAS + "/{cameraId}")
    class GetByIdTests {

        @Test
        @DisplayName("returns camera by ID")
        void getById_existingCamera_returnsCamera() throws Exception {
            Camera camera = createCamera("Sony", "A7 IV");

            mockMvc.perform(get(ApiPaths.CAMERAS + "/" + camera.getCameraId())
                            .with(authenticated(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cameraId").value(camera.getCameraId().toString()))
                    .andExpect(jsonPath("$.brand").value("Sony"));
        }

        @Test
        @DisplayName("returns 404 for non-existent camera")
        void getById_notFound_returns404() throws Exception {
            mockMvc.perform(get(ApiPaths.CAMERAS + "/00000000-0000-0000-0000-000000000001")
                            .with(authenticated(customer)))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/cameras/{cameraId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT " + ApiPaths.CAMERAS + "/{cameraId}")
    class UpdateTests {

        @Test
        @DisplayName("admin can update a camera")
        void update_asAdmin_returnsUpdated() throws Exception {
            Camera camera = createCamera("Sony", "A7 III");

            CameraDTO updateDto = validCameraDto();
            updateDto.setModelName("A7 IV");
            updateDto.setMaxIso(51200);

            mockMvc.perform(put(ApiPaths.CAMERAS + "/" + camera.getCameraId())
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modelName").value("A7 IV"))
                    .andExpect(jsonPath("$.maxIso").value(51200));
        }

        @Test
        @DisplayName("customer gets 403 when updating")
        void update_asCustomer_returns403() throws Exception {
            Camera camera = createCamera("Sony", "A7 III");

            mockMvc.perform(put(ApiPaths.CAMERAS + "/" + camera.getCameraId())
                            .with(authenticated(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validCameraDto())))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/cameras/{cameraId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE " + ApiPaths.CAMERAS + "/{cameraId}")
    class DeleteTests {

        @Test
        @DisplayName("admin can delete a camera and gets 204")
        void delete_asAdmin_returns204() throws Exception {
            Camera camera = createCamera("Sony", "A7 IV");

            mockMvc.perform(delete(ApiPaths.CAMERAS + "/" + camera.getCameraId())
                            .with(authenticated(admin)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(ApiPaths.CAMERAS + "/" + camera.getCameraId())
                            .with(authenticated(admin)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("customer gets 403 when deleting")
        void delete_asCustomer_returns403() throws Exception {
            Camera camera = createCamera("Sony", "A7 IV");

            mockMvc.perform(delete(ApiPaths.CAMERAS + "/" + camera.getCameraId())
                            .with(authenticated(customer)))
                    .andExpect(status().isForbidden());
        }
    }
}
