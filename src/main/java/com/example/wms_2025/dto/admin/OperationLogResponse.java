package com.example.wms_2025.dto.admin;

import java.time.LocalDateTime;

public record OperationLogResponse(Long id, String operator, String module, String action, String details,
        LocalDateTime createdAt) {
}
