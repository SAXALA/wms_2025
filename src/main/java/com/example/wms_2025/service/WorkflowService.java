package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.ApprovalFlow;
import com.example.wms_2025.domain.entity.ApprovalNode;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.ApprovalResult;
import com.example.wms_2025.domain.enums.ApprovalStatus;
import com.example.wms_2025.domain.enums.BusinessType;
import com.example.wms_2025.domain.enums.RoleCode;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.ApprovalFlowRepository;
import com.example.wms_2025.repository.ApprovalNodeRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final ApprovalFlowRepository approvalFlowRepository;
    private final ApprovalNodeRepository approvalNodeRepository;

    @Transactional
    public ApprovalFlow startFlow(User applicant, BusinessType businessType, User approver) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setApplicant(applicant);
        flow.setApprover(approver);
        flow.setBusinessType(businessType);
        flow.setStatus(ApprovalStatus.APPROVING);

        ApprovalNode node = new ApprovalNode();
        node.setFlow(flow);
        node.setApprover(approver);
        node.setApprovalResult(ApprovalResult.PENDING);
        flow.getNodes().add(node);

        return approvalFlowRepository.save(flow);
    }

    @Transactional
    public ApprovalFlow recordDecision(Long flowId, User approver, boolean approved, String comment) {
        Objects.requireNonNull(flowId, "flowId is required");

        ApprovalFlow flow = approvalFlowRepository.findById(flowId)
                .orElseThrow(() -> new BusinessException("Approval flow not found"));
        User assignedApprover = flow.getApprover();
        boolean isAssignedApprover = assignedApprover.getId().equals(approver.getId());
        boolean isAdmin = approver.getRoles().stream()
                .anyMatch(role -> role.getRoleCode() == RoleCode.ADMIN);

        if (!isAssignedApprover && !isAdmin) {
            throw new BusinessException("User is not assigned approver");
        }

        ApprovalNode node = flow.getNodes().stream()
                .filter(n -> n.getApprover().getId().equals(assignedApprover.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Approval node missing"));

        if (!isAssignedApprover && isAdmin) {
            node.setApprover(approver);
            flow.setApprover(approver);
        }
        node.setApprovalResult(approved ? ApprovalResult.APPROVED : ApprovalResult.REJECTED);
        node.setComment(comment);
        node.setApprovalTime(LocalDateTime.now());
        approvalNodeRepository.save(node);

        flow.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        return approvalFlowRepository.save(flow);
    }

    public List<ApprovalFlow> findPendingForApprover(User approver) {
        return approvalFlowRepository.findByApproverAndStatusIn(
                approver,
                List.of(ApprovalStatus.SUBMITTED, ApprovalStatus.APPROVING));
    }
}
