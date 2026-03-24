package com.camerarental.backend.controller;

import com.camerarental.backend.config.ApiPaths;
import com.camerarental.backend.config.AppConstants;
import com.camerarental.backend.payload.CameraDTO;
import com.camerarental.backend.payload.base.PagedResponse;
import com.camerarental.backend.security.services.UserDetailsImpl;
import com.camerarental.backend.service.CameraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.CAMERAS)
@RequiredArgsConstructor
@Tag(name = "Camera", description = "Manage Cameras")
public class CameraController {

    private final CameraService cameraService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Create a Camera", description = "Creates a camera in the database. Admin use only")
    public ResponseEntity<CameraDTO> create(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                            @Valid @RequestBody CameraDTO dto) {
        return new ResponseEntity<>(cameraService.createCamera(currentUser.getId(), dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDOR') or hasRole('CUSTOMER')")
    @GetMapping
    @Operation(summary = "Get all Cameras",
            description = "Returns a paginated list of cameras. By default only active cameras are returned. "
                    + "Admins can pass includeInactive=true to see deactivated cameras as well.")
    public ResponseEntity<PagedResponse<CameraDTO>> getAll(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) @Min(value = 0, message = "pageNumber must be >= 0") Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) @Min(value = 1, message = "pageSize must be >= 1") Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CAMERAS_BY) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder) {
        PagedResponse<CameraDTO> response = cameraService.getCameras(search, includeInactive,
                pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDOR') or hasRole('CUSTOMER')")
    @GetMapping("/{cameraId}")
    @Operation(summary = "Get a Camera by ID", description = "Returns a single camera by its ID")
    public ResponseEntity<CameraDTO> getById(@PathVariable UUID cameraId) {
        CameraDTO dto = cameraService.getCamera(cameraId);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{cameraId}")
    @Operation(summary = "Update a Camera", description = "Updates an existing camera. Admin use only")
    public ResponseEntity<CameraDTO> update(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                            @Valid @RequestBody CameraDTO dto,
                                            @PathVariable UUID cameraId) {
        CameraDTO updatedDTO = cameraService.updateCamera(currentUser.getId(), cameraId, dto);
        return ResponseEntity.ok(updatedDTO);
    }

    /**
     * Permanently removes a camera from the catalog.
     *
     * <h3>This endpoint is rarely used in production</h3>
     *
     * <p>In a real camera-rental business, catalog items are almost never
     * hard-deleted.  Every camera that has been rented, insured, or
     * depreciated is linked to historical records (rental agreements,
     * maintenance logs, revenue reports).  Deleting the catalog entry
     * would orphan that data and break financial audits.</p>
     *
     * <p>The <strong>preferred workflow</strong> is to deactivate the
     * camera via {@code PATCH /{cameraId}/deactivate}, which sets
     * {@code isActive = false} and hides it from customer-facing
     * listings while preserving all history.</p>
     *
     * <p>Hard-delete exists strictly as a <strong>data-cleanup</strong>
     * tool — for example, removing a camera that was created by mistake
     * or a duplicate entry that was never rented out.</p>
     *
     * <h3>Safety checks</h3>
     *
     * <p>The service rejects the delete with {@code 409 Conflict} if
     * physical units still exist under this camera's inventory.  The
     * admin must retire or remove all units first.  An empty inventory
     * record (zero units) is cascade-deleted automatically.</p>
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{cameraId}")
    @Operation(
            summary = "Hard-delete a camera (rarely used)",
            description = "Permanently removes a camera from the catalog. "
                    + "Rejected with 409 if physical units still exist. "
                    + "Prefer PATCH /{cameraId}/deactivate for normal retirement."
    )
    public ResponseEntity<Void> delete(@PathVariable UUID cameraId) {
        cameraService.deleteCamera(cameraId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Soft-deletes a camera by marking it inactive. This is the
     * recommended way to retire a camera from the catalog.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{cameraId}/deactivate")
    @Operation(
            summary = "Deactivate a camera (recommended)",
            description = "Sets isActive=false so the camera no longer appears in "
                    + "customer-facing listings but all historical data is preserved."
    )
    public ResponseEntity<CameraDTO> deactivate(@PathVariable UUID cameraId) {
        CameraDTO dto = cameraService.deactivateCamera(cameraId);
        return ResponseEntity.ok(dto);
    }
}
