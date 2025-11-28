package com.example.wms_2025.dto.report;

import java.time.LocalDate;

public record OperationTrendPoint(
        LocalDate date,
        int inCount,
        int outCount,
        int approvalCount) {
}
