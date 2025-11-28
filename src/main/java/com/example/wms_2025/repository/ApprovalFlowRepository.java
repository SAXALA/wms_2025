package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.ApprovalFlow;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.ApprovalStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long> {
    List<ApprovalFlow> findByApproverAndStatusIn(User approver, List<ApprovalStatus> statuses);
}
