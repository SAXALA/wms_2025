package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.ProcurementApplication;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.ProcurementStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementApplicationRepository extends JpaRepository<ProcurementApplication, Long> {
    List<ProcurementApplication> findByApplicant(User applicant);

    List<ProcurementApplication> findByStatusIn(List<ProcurementStatus> statuses);
}
