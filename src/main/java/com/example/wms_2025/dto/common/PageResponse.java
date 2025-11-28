package com.example.wms_2025.dto.common;

import java.util.List;

public record PageResponse<T>(long totalElements, int totalPages, int pageNumber, int pageSize, List<T> content) {

    public static <T> PageResponse<T> of(long totalElements, int totalPages, int pageNumber, int pageSize,
            List<T> content) {
        return new PageResponse<>(totalElements, totalPages, pageNumber, pageSize, content);
    }
}
