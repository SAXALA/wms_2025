package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.Inventory;
import com.example.wms_2025.domain.entity.InventoryApplication;
import com.example.wms_2025.domain.entity.OperationReport;
import com.example.wms_2025.domain.entity.ProcurementApplication;
import com.example.wms_2025.domain.enums.InventoryApplicationStatus;
import com.example.wms_2025.domain.enums.InventoryType;
import com.example.wms_2025.domain.enums.ProcurementStatus;
import com.example.wms_2025.dto.report.ApprovalStatsResponse;
import com.example.wms_2025.dto.report.InventorySummaryResponse;
import com.example.wms_2025.dto.report.OperationTrendPoint;
import com.example.wms_2025.dto.report.OperationTrendsResponse;
import com.example.wms_2025.repository.InventoryApplicationRepository;
import com.example.wms_2025.repository.InventoryRepository;
import com.example.wms_2025.repository.OperationReportRepository;
import com.example.wms_2025.repository.ProcurementApplicationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final InventoryRepository inventoryRepository;
    private final ProcurementApplicationRepository procurementApplicationRepository;
    private final InventoryApplicationRepository inventoryApplicationRepository;
    private final OperationReportRepository operationReportRepository;

    public InventorySummaryResponse generateInventorySummary() {
        List<Inventory> inventories = inventoryRepository.findAll();
        BigDecimal totalValue = inventories.stream()
                .map(inv -> BigDecimal.valueOf(inv.getCurrentStock()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal turnoverRate = inventories.stream()
                .filter(inv -> inv.getSafetyStock() > 0)
                .map(inv -> BigDecimal.valueOf(inv.getCurrentStock())
                        .divide(BigDecimal.valueOf(inv.getSafetyStock()), 4, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> lowStockWarnings = inventories.stream()
                .filter(inv -> inv.getCurrentStock() < inv.getSafetyStock())
                .map(inv -> "Product " + inv.getProductId() + " below safety stock")
                .toList();
        List<String> obsoleteItems = inventories.stream()
                .filter(inv -> inv.getCurrentStock() - inv.getLockedStock() > inv.getSafetyStock() * 3)
                .map(inv -> "Product " + inv.getProductId())
                .toList();
        return new InventorySummaryResponse(totalValue, turnoverRate, lowStockWarnings, obsoleteItems);
    }

    public ApprovalStatsResponse generateApprovalStats(LocalDate start, LocalDate end) {
        List<ProcurementApplication> procurementApplications = procurementApplicationRepository.findAll();
        long procurementApprovals = procurementApplications.stream()
                .filter(app -> within(app.getCreatedAt(), start, end))
                .filter(app -> app.getStatus() == ProcurementStatus.APPROVED)
                .count();
        long procurementRejections = procurementApplications.stream()
                .filter(app -> within(app.getCreatedAt(), start, end))
                .filter(app -> app.getStatus() == ProcurementStatus.REJECTED)
                .count();

        List<InventoryApplication> inApplications = inventoryApplicationRepository.findByTypeAndStatusIn(
                InventoryType.IN,
                List.of(InventoryApplicationStatus.APPROVED, InventoryApplicationStatus.COMPLETED));
        long inventoryInApprovals = inApplications.stream()
                .filter(app -> within(app.getCreatedAt(), start, end))
                .count();
        List<InventoryApplication> outApplications = inventoryApplicationRepository.findByTypeAndStatusIn(
                InventoryType.OUT,
                List.of(InventoryApplicationStatus.APPROVED, InventoryApplicationStatus.COMPLETED));
        long inventoryOutApprovals = outApplications.stream()
                .filter(app -> within(app.getCreatedAt(), start, end))
                .count();

        long totalDecisions = procurementApprovals + procurementRejections;
        BigDecimal approvalRate = totalDecisions == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(procurementApprovals)
                        .divide(BigDecimal.valueOf(totalDecisions), 2, RoundingMode.HALF_UP);

        double averageApprovalHours = procurementApplications.stream()
                .filter(app -> app.getApprovalFlow() != null && app.getApprovalFlow().getUpdatedAt() != null)
                .filter(app -> within(app.getCreatedAt(), start, end))
                .mapToDouble(app -> ChronoUnit.HOURS.between(app.getCreatedAt(), app.getApprovalFlow().getUpdatedAt()))
                .average()
                .orElse(0.0);

        return new ApprovalStatsResponse(
                procurementApprovals,
                inventoryInApprovals,
                inventoryOutApprovals,
                approvalRate,
                averageApprovalHours);
    }

    public OperationTrendsResponse generateOperationTrends(int days) {
        LocalDate threshold = LocalDate.now().minusDays(days);
        List<OperationTrendPoint> points = operationReportRepository.findAll().stream()
                .filter(report -> report.getReportDate().isAfter(threshold))
                .sorted(Comparator.comparing(OperationReport::getReportDate))
                .map(report -> new OperationTrendPoint(report.getReportDate(),
                        defaultValue(report.getInCount()),
                        defaultValue(report.getOutCount()),
                        defaultValue(report.getApprovalCount())))
                .collect(Collectors.toList());
        return new OperationTrendsResponse(points);
    }

    private boolean within(LocalDateTime createdAt, LocalDate start, LocalDate end) {
        if (createdAt == null) {
            return false;
        }
        LocalDate date = createdAt.toLocalDate();
        boolean afterStart = start == null || !date.isBefore(start);
        boolean beforeEnd = end == null || !date.isAfter(end);
        return afterStart && beforeEnd;
    }

    private int defaultValue(Integer value) {
        return value == null ? 0 : value;
    }
}
