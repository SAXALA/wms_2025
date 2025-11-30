package com.example.wms_2025.controller;

import com.example.wms_2025.domain.enums.InventoryApplicationStatus;
import com.example.wms_2025.domain.enums.InventoryType;
import com.example.wms_2025.dto.common.ApiResponse;
import com.example.wms_2025.dto.inventory.ApproveInventoryRequest;
import com.example.wms_2025.dto.inventory.CreateInventoryApplicationRequest;
import com.example.wms_2025.dto.inventory.ExecuteInventoryRequest;
import com.example.wms_2025.dto.inventory.InventoryApplicationResponse;
import com.example.wms_2025.service.InventoryService;
import com.example.wms_2025.dto.inventory.InventoryStockResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/in-applications")
    public ResponseEntity<ApiResponse<InventoryApplicationResponse>> createIn(
            @Valid @RequestBody CreateInventoryApplicationRequest request) {
        InventoryApplicationResponse response = inventoryService.createApplication(InventoryType.IN, request);
        return ResponseEntity.ok(ApiResponse.ok("In-bound application created", response));
    }

    @PutMapping("/in-applications/{id}/approve")
    public ResponseEntity<ApiResponse<InventoryApplicationResponse>> approveIn(
            @PathVariable Long id,
            @Valid @RequestBody ApproveInventoryRequest request) {
        InventoryApplicationResponse response = inventoryService.approve(id, request);
        return ResponseEntity.ok(ApiResponse.ok("In-bound approval processed", response));
    }

    @PostMapping("/in-applications/{id}/execute")
    public ResponseEntity<ApiResponse<InventoryApplicationResponse>> executeIn(
            @PathVariable Long id,
            @Valid @RequestBody ExecuteInventoryRequest request) {
        InventoryApplicationResponse response = inventoryService.execute(id, request);
        return ResponseEntity.ok(ApiResponse.ok("In-bound execution completed", response));
    }

    @PostMapping("/out-applications")
    public ResponseEntity<ApiResponse<InventoryApplicationResponse>> createOut(
            @Valid @RequestBody CreateInventoryApplicationRequest request) {
        InventoryApplicationResponse response = inventoryService.createApplication(InventoryType.OUT, request);
        return ResponseEntity.ok(ApiResponse.ok("Out-bound application created", response));
    }

    @PutMapping("/out-applications/{id}/approve")
    public ResponseEntity<ApiResponse<InventoryApplicationResponse>> approveOut(
            @PathVariable Long id,
            @Valid @RequestBody ApproveInventoryRequest request) {
        InventoryApplicationResponse response = inventoryService.approve(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Out-bound approval processed", response));
    }

    @PostMapping("/out-applications/{id}/execute")
    public ResponseEntity<ApiResponse<InventoryApplicationResponse>> executeOut(
            @PathVariable Long id,
            @Valid @RequestBody ExecuteInventoryRequest request) {
        InventoryApplicationResponse response = inventoryService.execute(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Out-bound execution completed", response));
    }

    @GetMapping("/in-applications")
    public ResponseEntity<ApiResponse<List<InventoryApplicationResponse>>> listIn() {
        List<InventoryApplicationResponse> responses = inventoryService.listByType(
                InventoryType.IN,
                List.of(InventoryApplicationStatus.PENDING_APPROVAL, InventoryApplicationStatus.APPROVED,
                        InventoryApplicationStatus.EXECUTING, InventoryApplicationStatus.COMPLETED));
        return ResponseEntity.ok(ApiResponse.ok("Query successful", responses));
    }

    @GetMapping("/out-applications")
    public ResponseEntity<ApiResponse<List<InventoryApplicationResponse>>> listOut() {
        List<InventoryApplicationResponse> responses = inventoryService.listByType(
                InventoryType.OUT,
                List.of(InventoryApplicationStatus.PENDING_APPROVAL, InventoryApplicationStatus.APPROVED,
                        InventoryApplicationStatus.EXECUTING, InventoryApplicationStatus.COMPLETED));
        return ResponseEntity.ok(ApiResponse.ok("Query successful", responses));
    }

    @GetMapping("/stocks")
    public ResponseEntity<ApiResponse<List<InventoryStockResponse>>> listStocks() {
        List<InventoryStockResponse> responses = inventoryService.listStocks();
        return ResponseEntity.ok(ApiResponse.ok("Inventory snapshot fetched", responses));
    }
}
