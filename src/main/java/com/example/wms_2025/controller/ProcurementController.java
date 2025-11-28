package com.example.wms_2025.controller;

import com.example.wms_2025.dto.common.ApiResponse;
import com.example.wms_2025.dto.procurement.ApproveProcurementRequest;
import com.example.wms_2025.dto.procurement.CreateProcurementRequest;
import com.example.wms_2025.dto.procurement.ProcurementApplicationResponse;
import com.example.wms_2025.service.ProcurementService;
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
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    @PostMapping("/applications")
    public ResponseEntity<ApiResponse<ProcurementApplicationResponse>> createApplication(
            @Valid @RequestBody CreateProcurementRequest request) {
        ProcurementApplicationResponse response = procurementService.createApplication(request);
        return ResponseEntity.ok(ApiResponse.ok("Procurement application created", response));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<ProcurementApplicationResponse>>> myApplications() {
        List<ProcurementApplicationResponse> responses = procurementService.myApplications();
        return ResponseEntity.ok(ApiResponse.ok("Query successful", responses));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApiResponse<ProcurementApplicationResponse>> applicationDetail(@PathVariable Long id) {
        ProcurementApplicationResponse response = procurementService.getApplication(id);
        return ResponseEntity.ok(ApiResponse.ok("Query successful", response));
    }

    @PutMapping("/applications/{id}/approve")
    public ResponseEntity<ApiResponse<ProcurementApplicationResponse>> approve(
            @PathVariable Long id,
            @Valid @RequestBody ApproveProcurementRequest request) {
        ProcurementApplicationResponse response = procurementService.approveApplication(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Approval processed", response));
    }

    @GetMapping("/applications/pending")
    public ResponseEntity<ApiResponse<List<ProcurementApplicationResponse>>> pending() {
        List<ProcurementApplicationResponse> responses = procurementService.pendingApprovals();
        return ResponseEntity.ok(ApiResponse.ok("Pending approvals fetched", responses));
    }
}
