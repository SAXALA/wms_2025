package com.example.wms_2025.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordResetRequest(@NotBlank @Size(min = 8, max = 128) String newPassword) {
}
