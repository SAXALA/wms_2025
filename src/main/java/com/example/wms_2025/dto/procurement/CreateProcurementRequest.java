package com.example.wms_2025.dto.procurement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateProcurementRequest(
        @NotBlank String title,
        @NotNull BigDecimal totalAmount,
        @Valid @Size(min = 1) List<ProcurementItemRequest> items) {
}
