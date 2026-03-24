package com.camerarental.backend.controller;

import com.camerarental.backend.config.AbstractPostgresIT;
import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.config.PostgresIntegrationTest;
import com.camerarental.backend.model.entity.User;
import com.camerarental.backend.payload.BusinessHoursDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PostgresIntegrationTest
class BusinessHoursControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    private User admin;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        admin = createAdmin();
    }

    private BusinessHoursDTO mondayOpen() {
        BusinessHoursDTO dto = new BusinessHoursDTO();
        dto.setDayOfWeek(DayOfWeek.MONDAY);
        dto.setOpenTime(LocalTime.of(9, 0));
        dto.setCloseTime(LocalTime.of(17, 0));
        dto.setClosed(false);
        return dto;
    }

    private BusinessHoursDTO sundayClosed() {
        BusinessHoursDTO dto = new BusinessHoursDTO();
        dto.setDayOfWeek(DayOfWeek.SUNDAY);
        dto.setClosed(true);
        return dto;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/business-hours
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST " + ApiPaths.BUSINESS_HOURS)
    class CreateTests {

        @Test
        @DisplayName("admin creates business hours and gets 201")
        void create_asAdmin_returns201() throws Exception {
            mockMvc.perform(post(ApiPaths.BUSINESS_HOURS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(mondayOpen())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.businessHoursId").isString())
                    .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                    .andExpect(jsonPath("$.openTime").value("09:00:00"))
                    .andExpect(jsonPath("$.closeTime").value("17:00:00"))
                    .andExpect(jsonPath("$.closed").value(false));
        }

        @Test
        @DisplayName("unauthenticated user gets 403 — business-hours is permitAll but @PreAuthorize requires ADMIN")
        void create_unauthenticated_returns403() throws Exception {
            mockMvc.perform(post(ApiPaths.BUSINESS_HOURS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(mondayOpen())))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/business-hours
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET " + ApiPaths.BUSINESS_HOURS)
    class GetAllTests {

        @Test
        @DisplayName("public access returns all business hours")
        void getAll_public() throws Exception {
            mockMvc.perform(post(ApiPaths.BUSINESS_HOURS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(mondayOpen())))
                    .andExpect(status().isCreated());

            mockMvc.perform(post(ApiPaths.BUSINESS_HOURS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(sundayClosed())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get(ApiPaths.BUSINESS_HOURS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/business-hours/{day}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET " + ApiPaths.BUSINESS_HOURS + "/{day}")
    class GetByDayTests {

        @Test
        @DisplayName("returns hours for a specific day (case-insensitive)")
        void getByDay_caseInsensitive() throws Exception {
            mockMvc.perform(post(ApiPaths.BUSINESS_HOURS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(mondayOpen())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get(ApiPaths.BUSINESS_HOURS + "/monday"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"));
        }

        @Test
        @DisplayName("returns 404 for day without configured hours")
        void getByDay_notFound() throws Exception {
            mockMvc.perform(get(ApiPaths.BUSINESS_HOURS + "/WEDNESDAY"))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/business-hours/{day}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT " + ApiPaths.BUSINESS_HOURS + "/{day}")
    class UpdateTests {

        @Test
        @DisplayName("admin can update hours for a day")
        void update_asAdmin() throws Exception {
            mockMvc.perform(post(ApiPaths.BUSINESS_HOURS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(mondayOpen())))
                    .andExpect(status().isCreated());

            BusinessHoursDTO updated = mondayOpen();
            updated.setOpenTime(LocalTime.of(10, 0));
            updated.setCloseTime(LocalTime.of(18, 0));

            mockMvc.perform(put(ApiPaths.BUSINESS_HOURS + "/MONDAY")
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(updated)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openTime").value("10:00:00"))
                    .andExpect(jsonPath("$.closeTime").value("18:00:00"));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/business-hours/{day}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE " + ApiPaths.BUSINESS_HOURS + "/{day}")
    class DeleteTests {

        @Test
        @DisplayName("admin can delete business hours and gets 204")
        void delete_asAdmin_returns204() throws Exception {
            mockMvc.perform(post(ApiPaths.BUSINESS_HOURS)
                            .with(authenticated(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(mondayOpen())))
                    .andExpect(status().isCreated());

            mockMvc.perform(delete(ApiPaths.BUSINESS_HOURS + "/MONDAY")
                            .with(authenticated(admin)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(ApiPaths.BUSINESS_HOURS + "/MONDAY"))
                    .andExpect(status().isNotFound());
        }
    }
}
