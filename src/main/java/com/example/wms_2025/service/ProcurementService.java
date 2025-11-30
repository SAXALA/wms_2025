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
import com.example.wms_2025.dto.product.ProductResponse;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.ProcurementApplicationRepository;
import com.example.wms_2025.service.validator.ProcurementValidator;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final ProductService productService;
    private final OperationLogService operationLogService;

    private static final String MODULE_PROCUREMENT = "采购管理";

    @Transactional
    @PreAuthorize("hasAnyRole('PURCHASER','ADMIN')")
    public ProcurementApplicationResponse createApplication(CreateProcurementRequest request) {
        User applicant = userService.getCurrentUser(RoleCode.PURCHASER, RoleCode.ADMIN);
        var validation = procurementValidator.validateCreation(request);

        ProcurementApplication application = new ProcurementApplication();
        application.setApplicant(applicant);
        application.setTitle(request.title());
        application.setTotalAmount(validation.totalAmount());
        application.setStatus(ProcurementStatus.SUBMITTED);

        request.items().forEach(itemRequest -> application.addItem(toItemEntity(itemRequest, validation.products())));

        User manager = userService.getManager();
        ApprovalFlow flow = workflowService.startFlow(applicant, BusinessType.PROCUREMENT, manager);
        application.setApprovalFlow(flow);

        ProcurementApplication saved = procurementApplicationRepository.save(application);
        operationLogService.record(MODULE_PROCUREMENT, "CREATE",
                "创建采购申请" + (saved.getId() != null ? "#" + saved.getId() : ""));
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ProcurementApplicationResponse approveApplication(Long applicationId, ApproveProcurementRequest request) {
        Objects.requireNonNull(applicationId, "Application id is required");
        User approver = userService.getCurrentUser(RoleCode.MANAGER, RoleCode.ADMIN);
        ProcurementApplication application = procurementApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Procurement application not found"));
        boolean isAdmin = approver.getRoles().stream()
                .anyMatch(role -> role.getRoleCode() == RoleCode.ADMIN);
        if (!isAdmin && !isManagerAssigned(application, approver)) {
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
        String action = request.approved() ? "APPROVE" : "REJECT";
        operationLogService.record(MODULE_PROCUREMENT, action,
                (request.approved() ? "审批通过采购申请" : "驳回采购申请")
                        + (saved.getId() != null ? "#" + saved.getId() : ""));
        return toResponse(saved);
    }

    @PreAuthorize("hasAnyRole('PURCHASER','MANAGER','ADMIN')")
    public List<ProcurementApplicationResponse> myApplications() {
        User applicant = userService.getCurrentUser(RoleCode.PURCHASER, RoleCode.MANAGER, RoleCode.ADMIN);
        boolean isManagerOrAdmin = applicant.getRoles().stream()
                .anyMatch(role -> role.getRoleCode() == RoleCode.MANAGER || role.getRoleCode() == RoleCode.ADMIN);
        if (isManagerOrAdmin) {
            return procurementApplicationRepository.findAll().stream()
                    .map(this::toResponse)
                    .toList();
        }
        return procurementApplicationRepository.findByApplicant(applicant).stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('PURCHASER','MANAGER','ADMIN')")
    public ProcurementApplicationResponse getApplication(Long id) {
        Objects.requireNonNull(id, "Application id is required");
        ProcurementApplication application = procurementApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Procurement application not found"));
        return toResponse(application);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
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

    private ProcurementItem toItemEntity(ProcurementItemRequest request, Map<Long, ProductResponse> productMap) {
        ProcurementItem item = new ProcurementItem();
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        ProductResponse product = productMap.get(request.productId());
        item.setExpectedPrice(request.expectedPrice() != null ? request.expectedPrice() : product.price());
        return item;
    }

    private ProcurementApplicationResponse toResponse(ProcurementApplication entity) {
        Set<Long> productIds = entity.getItems().stream()
                .map(ProcurementItem::getProductId)
                .collect(Collectors.toSet());
        var productMap = productService.findAsMap(productIds);
        List<ProcurementItemResponse> items = entity.getItems().stream()
                .map(item -> {
                    ProductResponse product = productMap.get(item.getProductId());
                    return new ProcurementItemResponse(item.getId(), item.getProductId(), item.getQuantity(),
                            item.getExpectedPrice(),
                            product != null ? product.sku() : null,
                            product != null ? product.name() : null,
                            product != null ? product.unit() : null);
                })
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
