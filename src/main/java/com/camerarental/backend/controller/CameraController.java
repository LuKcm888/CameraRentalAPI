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
    @Operation(summary = "Get all Cameras", description = "Returns a paginated list of cameras with optional search")
    public ResponseEntity<PagedResponse<CameraDTO>> getAll(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) @Min(value = 0, message = "pageNumber must be >= 0") Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) @Min(value = 1, message = "pageSize must be >= 1") Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CAMERAS_BY) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder) {
        PagedResponse<CameraDTO> response = cameraService.getCameras(search, pageNumber, pageSize, sortBy, sortOrder);
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

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{cameraId}")
    @Operation(summary = "Delete a Camera", description = "Deletes a camera from the database. Admin use only")
    public ResponseEntity<Void> delete(@PathVariable UUID cameraId) {
        cameraService.deleteCamera(cameraId);
        return ResponseEntity.noContent().build();
    }
}
