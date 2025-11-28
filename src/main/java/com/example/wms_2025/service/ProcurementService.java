package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.ApprovalFlow;
import com.example.wms_2025.domain.entity.ProcurementApplication;
import com.example.wms_2025.domain.entity.ProcurementItem;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.BusinessType;
import com.example.wms_2025.domain.enums.ProcurementStatus;
import com.example.wms_2025.domain.enums.RoleCode;
import com.example.wms_2025.dto.procurement.ApproveProcurementRequest;
import com.example.wms_2025.dto.procurement.CreateProcurementRequest;
import com.example.wms_2025.dto.procurement.ProcurementApplicationResponse;
import com.example.wms_2025.dto.procurement.ProcurementItemRequest;
import com.example.wms_2025.dto.procurement.ProcurementItemResponse;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.ProcurementApplicationRepository;
import com.example.wms_2025.service.validator.ProcurementValidator;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcurementService {

    private final ProcurementApplicationRepository procurementApplicationRepository;
    private final UserService userService;
    private final WorkflowService workflowService;
    private final ProcurementValidator procurementValidator;

    @Transactional
    @PreAuthorize("hasRole('PURCHASER')")
    public ProcurementApplicationResponse createApplication(CreateProcurementRequest request) {
        User applicant = userService.getCurrentUser();
        procurementValidator.validateCreation(request);

        ProcurementApplication application = new ProcurementApplication();
        application.setApplicant(applicant);
        application.setTitle(request.title());
        application.setTotalAmount(request.totalAmount());
        application.setStatus(ProcurementStatus.SUBMITTED);

        request.items().forEach(itemRequest -> application.addItem(toItemEntity(itemRequest)));

        User manager = userService.getManager();
        ApprovalFlow flow = workflowService.startFlow(applicant, BusinessType.PROCUREMENT, manager);
        application.setApprovalFlow(flow);

        ProcurementApplication saved = procurementApplicationRepository.save(application);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public ProcurementApplicationResponse approveApplication(Long applicationId, ApproveProcurementRequest request) {
        Objects.requireNonNull(applicationId, "Application id is required");
        User approver = userService.getCurrentUser();
        ProcurementApplication application = procurementApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Procurement application not found"));
        if (!isManagerAssigned(application, approver)) {
            throw new BusinessException("User not assigned to this approval");
        }
        if (application.getStatus() == ProcurementStatus.REJECTED
                || application.getStatus() == ProcurementStatus.APPROVED) {
            throw new BusinessException("Application already processed");
        }

        workflowService.recordDecision(application.getApprovalFlow().getId(), approver, request.approved(),
                request.comment());
        application.setStatus(request.approved() ? ProcurementStatus.APPROVED : ProcurementStatus.REJECTED);
        ProcurementApplication saved = procurementApplicationRepository.save(application);
        return toResponse(saved);
    }

    @PreAuthorize("hasAnyRole('PURCHASER','MANAGER')")
    public List<ProcurementApplicationResponse> myApplications() {
        User applicant = userService.getCurrentUser();
        boolean isManager = applicant.getRoles().stream().anyMatch(role -> role.getRoleCode() == RoleCode.MANAGER);
        if (isManager) {
            return procurementApplicationRepository.findAll().stream()
                    .map(this::toResponse)
                    .toList();
        }
        return procurementApplicationRepository.findByApplicant(applicant).stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('PURCHASER','MANAGER')")
    public ProcurementApplicationResponse getApplication(Long id) {
        Objects.requireNonNull(id, "Application id is required");
        ProcurementApplication application = procurementApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Procurement application not found"));
        return toResponse(application);
    }

    @PreAuthorize("hasRole('MANAGER')")
    public List<ProcurementApplicationResponse> pendingApprovals() {
        return procurementApplicationRepository
                .findByStatusIn(List.of(ProcurementStatus.SUBMITTED, ProcurementStatus.APPROVING)).stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isManagerAssigned(ProcurementApplication application, User approver) {
        ApprovalFlow flow = application.getApprovalFlow();
        return flow != null && flow.getApprover().getId().equals(approver.getId());
    }

    private ProcurementItem toItemEntity(ProcurementItemRequest request) {
        ProcurementItem item = new ProcurementItem();
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        item.setExpectedPrice(request.expectedPrice());
        return item;
    }

    private ProcurementApplicationResponse toResponse(ProcurementApplication entity) {
        List<ProcurementItemResponse> items = entity.getItems().stream()
                .map(item -> new ProcurementItemResponse(item.getId(), item.getProductId(), item.getQuantity(),
                        item.getExpectedPrice()))
                .collect(Collectors.toList());
        String applicantName = entity.getApplicant() != null ? entity.getApplicant().getRealName() : null;
        return new ProcurementApplicationResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getTotalAmount(),
                entity.getStatus(),
                applicantName,
                entity.getCreatedAt(),
                items);
    }
}
