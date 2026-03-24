package com.camerarental.backend.controller;

import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.config.AppConstants;
import com.camerarental.backend.payload.PhysicalUnitDTO;
import com.camerarental.backend.payload.base.PagedResponse;
import com.camerarental.backend.service.PhysicalUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.PHYSICAL_UNITS)
@RequiredArgsConstructor
@Tag(name = "Physical Units", description = "Manage individual camera units")
public class PhysicalUnitController {

    private final PhysicalUnitService physicalUnitService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Register a physical unit",
               description = "Adds a new physical camera unit to an inventory item. Admin only.")
    public ResponseEntity<PhysicalUnitDTO> create(@Valid @RequestBody PhysicalUnitDTO dto) {
        return new ResponseEntity<>(physicalUnitService.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDOR') or hasRole('CUSTOMER')")
    @GetMapping("/inventory/{inventoryItemId}")
    @Operation(summary = "List physical units for an inventory item",
               description = "Returns a paginated list of units for the given inventory item.")
    public ResponseEntity<PagedResponse<PhysicalUnitDTO>> getByInventoryItem(
            @PathVariable UUID inventoryItemId,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER)
            @Min(value = 0, message = "pageNumber must be >= 0") Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE)
            @Min(value = 1, message = "pageSize must be >= 1") Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_UNITS_BY) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder) {
        return ResponseEntity.ok(physicalUnitService.getByInventoryItem(
                inventoryItemId, pageNumber, pageSize, sortBy, sortOrder));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDOR') or hasRole('CUSTOMER')")
    @GetMapping("/{physicalUnitId}")
    @Operation(summary = "Get a physical unit by ID",
               description = "Returns a single physical unit.")
    public ResponseEntity<PhysicalUnitDTO> getById(@PathVariable UUID physicalUnitId) {
        return ResponseEntity.ok(physicalUnitService.getById(physicalUnitId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{physicalUnitId}")
    @Operation(summary = "Update a physical unit",
               description = "Updates serial number, condition, status, notes, and acquired date. Admin only.")
    public ResponseEntity<PhysicalUnitDTO> update(@PathVariable UUID physicalUnitId,
                                                  @Valid @RequestBody PhysicalUnitDTO dto) {
        return ResponseEntity.ok(physicalUnitService.update(physicalUnitId, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{physicalUnitId}")
    @Operation(summary = "Delete a physical unit",
               description = "Removes a physical unit from the system. Admin only.")
    public ResponseEntity<Void> delete(@PathVariable UUID physicalUnitId) {
        physicalUnitService.delete(physicalUnitId);
        return ResponseEntity.noContent().build();
    }
}
