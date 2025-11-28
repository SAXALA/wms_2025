package com.example.wms_2025.controller;

import com.example.wms_2025.dto.common.ApiResponse;
import com.example.wms_2025.dto.report.ApprovalStatsResponse;
import com.example.wms_2025.dto.report.InventorySummaryResponse;
import com.example.wms_2025.dto.report.OperationTrendsResponse;
import com.example.wms_2025.service.ReportService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/inventory-summary")
    public ResponseEntity<ApiResponse<InventorySummaryResponse>> inventorySummary() {
        InventorySummaryResponse response = reportService.generateInventorySummary();
        return ResponseEntity.ok(ApiResponse.ok("Inventory summary generated", response));
    }

    @GetMapping("/approval-stats")
    public ResponseEntity<ApiResponse<ApprovalStatsResponse>> approvalStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        ApprovalStatsResponse response = reportService.generateApprovalStats(start, end);
        return ResponseEntity.ok(ApiResponse.ok("Approval statistics generated", response));
    }

    @GetMapping("/operation-trends")
    public ResponseEntity<ApiResponse<OperationTrendsResponse>> operationTrends(
            @RequestParam(defaultValue = "30") int days) {
        OperationTrendsResponse response = reportService.generateOperationTrends(days);
        return ResponseEntity.ok(ApiResponse.ok("Operation trends generated", response));
    }
}
