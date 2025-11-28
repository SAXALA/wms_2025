package com.example.wms_2025.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @NotBlank @Size(max = 64) String realName,
        @Size(max = 64) String department) {
}
