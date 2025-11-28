package com.example.wms_2025.dto.procurement;

import java.math.BigDecimal;

public record ProcurementItemResponse(
        Long id,
        Long productId,
        Integer quantity,
        BigDecimal expectedPrice) {
}
