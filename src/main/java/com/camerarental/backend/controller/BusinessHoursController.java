package com.camerarental.backend.controller;

import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.payload.BusinessHoursDTO;
import com.camerarental.backend.service.BusinessHoursService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.BUSINESS_HOURS)
@RequiredArgsConstructor
@Tag(name = "Business Hours", description = "Manage weekly operating hours")
public class BusinessHoursController {

    private final BusinessHoursService businessHoursService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Create business hours for a day", description = "Admin only. Fails if the day already exists.")
    public ResponseEntity<BusinessHoursDTO> create(@Valid @RequestBody BusinessHoursDTO dto) {
        return new ResponseEntity<>(businessHoursService.create(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get weekly schedule", description = "Returns all days ordered Monday-Sunday. Public access.")
    public ResponseEntity<List<BusinessHoursDTO>> getAll() {
        return ResponseEntity.ok(businessHoursService.getAll());
    }

    @GetMapping("/{day}")
    @Operation(summary = "Get business hours by day", description = "Returns a single day's hours. Public access. Use MONDAY, TUESDAY, etc.")
    public ResponseEntity<BusinessHoursDTO> getByDay(@PathVariable DayOfWeek day) {
        return ResponseEntity.ok(businessHoursService.getByDay(day));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{day}")
    @Operation(summary = "Update business hours", description = "Admin only. Updates an existing day's open/close times.")
    public ResponseEntity<BusinessHoursDTO> update(@PathVariable DayOfWeek day,
                                                   @Valid @RequestBody BusinessHoursDTO dto) {
        return ResponseEntity.ok(businessHoursService.update(day, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{day}")
    @Operation(summary = "Delete business hours", description = "Admin only. Removes a day's entry.")
    public ResponseEntity<Void> delete(@PathVariable DayOfWeek day) {
        businessHoursService.delete(day);
        return ResponseEntity.noContent().build();
    }
}
