package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.InventoryReport;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReportRepository extends JpaRepository<InventoryReport, Long> {
    Optional<InventoryReport> findByReportDate(LocalDate reportDate);
}
