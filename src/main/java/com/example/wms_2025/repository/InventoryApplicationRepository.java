package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.InventoryApplication;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.InventoryApplicationStatus;
import com.example.wms_2025.domain.enums.InventoryType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryApplicationRepository extends JpaRepository<InventoryApplication, Long> {
    List<InventoryApplication> findByApplicant(User applicant);

    List<InventoryApplication> findByTypeAndStatusIn(InventoryType type, List<InventoryApplicationStatus> statuses);
}
