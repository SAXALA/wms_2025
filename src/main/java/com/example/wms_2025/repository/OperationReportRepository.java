package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.OperationReport;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationReportRepository extends JpaRepository<OperationReport, Long> {
    Optional<OperationReport> findByReportDate(LocalDate reportDate);
}
