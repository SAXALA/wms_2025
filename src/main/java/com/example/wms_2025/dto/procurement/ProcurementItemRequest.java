package com.example.wms_2025.dto.procurement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProcurementItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        BigDecimal expectedPrice) {
}
