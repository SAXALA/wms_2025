package com.example.wms_2025.dto.procurement;

import jakarta.validation.constraints.NotNull;

public record ApproveProcurementRequest(
        @NotNull Boolean approved,
        String comment) {
}
