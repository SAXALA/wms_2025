package com.example.wms_2025.dto.admin;

import com.example.wms_2025.domain.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(@NotNull UserStatus status) {
}
