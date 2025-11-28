package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OperationLogRepository
        extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {

    long countByOperatorId(Long operatorId);
}
