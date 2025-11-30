package com.example.wms_2025.controller;

import com.example.wms_2025.dto.common.ApiResponse;
import com.example.wms_2025.dto.warehouse.WarehouseLocationRequest;
import com.example.wms_2025.dto.warehouse.WarehouseLocationResponse;
import com.example.wms_2025.dto.warehouse.WarehouseLocationStatusRequest;
import com.example.wms_2025.service.WarehouseLocationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouse/locations")
@RequiredArgsConstructor
public class WarehouseLocationController {

    private final WarehouseLocationService warehouseLocationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseLocationResponse>>> list(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        List<WarehouseLocationResponse> responses = includeInactive
                ? warehouseLocationService.listAll()
                : warehouseLocationService.listActive();
        return ResponseEntity.ok(ApiResponse.ok("Locations fetched", responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseLocationResponse>> create(
            @Valid @RequestBody WarehouseLocationRequest request) {
        WarehouseLocationResponse response = warehouseLocationService.create(request);
        return ResponseEntity.ok(ApiResponse.ok("Location created", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseLocationResponse>> update(@PathVariable Long id,
            @Valid @RequestBody WarehouseLocationRequest request) {
        WarehouseLocationResponse response = warehouseLocationService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Location updated", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WarehouseLocationResponse>> updateStatus(@PathVariable Long id,
            @Valid @RequestBody WarehouseLocationStatusRequest request) {
        WarehouseLocationResponse response = warehouseLocationService.toggleStatus(id, request.active());
        return ResponseEntity.ok(ApiResponse.ok("Location status updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        warehouseLocationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Location deleted", null));
    }
}
