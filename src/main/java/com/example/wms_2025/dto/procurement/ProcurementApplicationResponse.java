package com.example.wms_2025.dto.procurement;

import com.example.wms_2025.domain.enums.ProcurementStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProcurementApplicationResponse(
        Long id,
        String title,
        BigDecimal totalAmount,
        ProcurementStatus status,
        String applicant,
        LocalDateTime createdAt,
        List<ProcurementItemResponse> items) {
}
