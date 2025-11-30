package com.example.wms_2025.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OperationLogArchiveResponse(String fileName, LocalDate date, long recordCount, LocalDateTime lastRecordAt) {
}
