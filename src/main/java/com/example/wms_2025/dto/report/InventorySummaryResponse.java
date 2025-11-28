package com.example.wms_2025.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record InventorySummaryResponse(
        BigDecimal totalInventoryValue,
        BigDecimal averageTurnoverRate,
        List<String> lowStockWarnings,
        List<String> obsoleteItems) {
}
