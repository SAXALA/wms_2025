package com.example.wms_2025.dto.warehouse;

import java.time.LocalDateTime;

public record WarehouseLocationResponse(
        Long id,
        String code,
        String name,
        String description,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
