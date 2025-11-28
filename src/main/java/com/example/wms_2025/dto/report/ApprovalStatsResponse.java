package com.example.wms_2025.dto.report;

import java.math.BigDecimal;

public record ApprovalStatsResponse(
        long procurementApprovals,
        long inventoryInApprovals,
        long inventoryOutApprovals,
        BigDecimal approvalRate,
        double averageApprovalHours) {
}
