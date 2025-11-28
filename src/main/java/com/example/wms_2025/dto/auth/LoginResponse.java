package com.example.wms_2025.dto.auth;

import java.util.Set;

public record LoginResponse(String token, String username, Set<String> roles) {
}
