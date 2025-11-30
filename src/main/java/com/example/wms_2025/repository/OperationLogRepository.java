package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.OperationLog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OperationLogRepository
        extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {

    long countByOperatorId(Long operatorId);

    @Query("""
            SELECT function('date', l.createdAt) AS logDate,
               COUNT(l) AS total,
               MAX(l.createdAt) AS lastTime
            FROM OperationLog l
            GROUP BY function('date', l.createdAt)
            ORDER BY function('date', l.createdAt) DESC
            """)
    List<OperationLogDailySummaryProjection> findDailySummaries(Pageable pageable);

    interface OperationLogDailySummaryProjection {
        LocalDate getLogDate();

        Long getTotal();

        LocalDateTime getLastTime();
    }
}
