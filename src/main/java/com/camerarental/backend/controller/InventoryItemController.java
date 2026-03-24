package com.camerarental.backend.controller;

import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.config.AppConstants;
import com.camerarental.backend.payload.InventoryItemDTO;
import com.camerarental.backend.payload.base.PagedResponse;
import com.camerarental.backend.service.InventoryItemService;
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
@RequestMapping(ApiPaths.INVENTORY)
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Manage inventory items (camera pricing & stock)")
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Create an inventory item",
               description = "Links a camera to its rental pricing. Admin only. One item per camera.")
    public ResponseEntity<InventoryItemDTO> create(@Valid @RequestBody InventoryItemDTO dto) {
        return new ResponseEntity<>(inventoryItemService.create(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDOR') or hasRole('CUSTOMER')")
    @GetMapping
    @Operation(summary = "List all inventory items",
               description = "Paginated list with computed unit counts.")
    public ResponseEntity<PagedResponse<InventoryItemDTO>> getAll(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER)
            @Min(value = 0, message = "pageNumber must be >= 0") Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE)
            @Min(value = 1, message = "pageSize must be >= 1") Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_INVENTORY_BY) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder) {
        return ResponseEntity.ok(inventoryItemService.getAll(pageNumber, pageSize, sortBy, sortOrder));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDOR') or hasRole('CUSTOMER')")
    @GetMapping("/{inventoryItemId}")
    @Operation(summary = "Get an inventory item by ID",
               description = "Returns a single item with computed unit counts.")
    public ResponseEntity<InventoryItemDTO> getById(@PathVariable UUID inventoryItemId) {
        return ResponseEntity.ok(inventoryItemService.getById(inventoryItemId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{inventoryItemId}")
    @Operation(summary = "Update inventory item pricing",
               description = "Updates daily rental price and replacement value. Admin only.")
    public ResponseEntity<InventoryItemDTO> update(@PathVariable UUID inventoryItemId,
                                                   @Valid @RequestBody InventoryItemDTO dto) {
        return ResponseEntity.ok(inventoryItemService.update(inventoryItemId, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{inventoryItemId}")
    @Operation(summary = "Delete an inventory item",
               description = "Removes the item and cascades to all physical units. Admin only.")
    public ResponseEntity<Void> delete(@PathVariable UUID inventoryItemId) {
        inventoryItemService.delete(inventoryItemId);
        return ResponseEntity.noContent().build();
    }
}
