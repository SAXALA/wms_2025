package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.ApprovalFlow;
import com.example.wms_2025.domain.entity.Inventory;
import com.example.wms_2025.domain.entity.InventoryApplication;
import com.example.wms_2025.domain.entity.InventoryItem;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.enums.BusinessType;
import com.example.wms_2025.domain.enums.InventoryApplicationStatus;
import com.example.wms_2025.domain.enums.InventoryType;
import com.example.wms_2025.dto.inventory.ApproveInventoryRequest;
import com.example.wms_2025.dto.inventory.CreateInventoryApplicationRequest;
import com.example.wms_2025.dto.inventory.ExecuteInventoryRequest;
import com.example.wms_2025.dto.inventory.InventoryApplicationResponse;
import com.example.wms_2025.dto.inventory.InventoryItemRequest;
import com.example.wms_2025.dto.inventory.InventoryItemResponse;
import com.example.wms_2025.dto.inventory.InventoryExecutionItem;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.InventoryApplicationRepository;
import com.example.wms_2025.repository.InventoryRepository;
import com.example.wms_2025.service.validator.InventoryValidator;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryApplicationRepository inventoryApplicationRepository;
    private final InventoryRepository inventoryRepository;
    private final UserService userService;
    private final WorkflowService workflowService;
    private final InventoryValidator inventoryValidator;

    @Transactional
    @PreAuthorize("hasRole('OPERATOR')")
    public InventoryApplicationResponse createApplication(InventoryType type,
            CreateInventoryApplicationRequest request) {
        if (request.type() != type) {
            throw new BusinessException("Request type mismatch with endpoint");
        }
        User applicant = userService.getCurrentUser();
        inventoryValidator.validateCreateRequest(request);

        InventoryApplication application = new InventoryApplication();
        application.setType(type);
        application.setApplicant(applicant);
        application.setReason(request.reason());
        application.setStatus(InventoryApplicationStatus.PENDING_APPROVAL);
        request.items().forEach(itemRequest -> application.addItem(toInventoryItem(itemRequest)));

        User manager = userService.getManager();
        ApprovalFlow flow = workflowService.startFlow(applicant,
                type == InventoryType.IN ? BusinessType.INVENTORY_IN : BusinessType.INVENTORY_OUT,
                manager);
        application.setApprovalFlow(flow);

        lockStockIfNeeded(type, request.items());

        InventoryApplication saved = inventoryApplicationRepository.save(application);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public InventoryApplicationResponse approve(Long applicationId, ApproveInventoryRequest request) {
        Objects.requireNonNull(applicationId, "Application id is required");
        InventoryApplication application = inventoryApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Inventory application not found"));
        if (application.getStatus() != InventoryApplicationStatus.PENDING_APPROVAL) {
            throw new BusinessException("Application already processed");
        }
        User approver = userService.getCurrentUser();
        workflowService.recordDecision(application.getApprovalFlow().getId(), approver, request.approved(),
                request.comment());
        application.setStatus(
                request.approved() ? InventoryApplicationStatus.APPROVED : InventoryApplicationStatus.REJECTED);
        InventoryApplication saved = inventoryApplicationRepository.save(application);
        if (!request.approved() && application.getType() == InventoryType.OUT) {
            releaseLockedStock(application);
        }
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('OPERATOR')")
    public InventoryApplicationResponse execute(Long applicationId, ExecuteInventoryRequest request) {
        Objects.requireNonNull(applicationId, "Application id is required");
        InventoryApplication application = inventoryApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Inventory application not found"));
        if (application.getStatus() != InventoryApplicationStatus.APPROVED) {
            throw new BusinessException("Application not approved");
        }

        Map<Long, Inventory> inventoryMap = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, inv -> inv));
        inventoryValidator.validateExecution(request, application.getType(), inventoryMap);

        Map<Long, InventoryItem> itemMap = application.getItems().stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, item -> item));

        application.setStatus(InventoryApplicationStatus.EXECUTING);
        request.items()
                .forEach(itemRequest -> applyExecution(application.getType(), inventoryMap, itemMap, itemRequest));
        application.setStatus(InventoryApplicationStatus.COMPLETED);
        InventoryApplication saved = inventoryApplicationRepository.save(application);
        return toResponse(saved);
    }

    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER')")
    public List<InventoryApplicationResponse> listByType(InventoryType type,
            List<InventoryApplicationStatus> statuses) {
        return inventoryApplicationRepository.findByTypeAndStatusIn(type, statuses).stream()
                .map(this::toResponse)
                .toList();
    }

    private void lockStockIfNeeded(InventoryType type, List<InventoryItemRequest> items) {
        if (type != InventoryType.OUT) {
            return;
        }
        Map<Long, Inventory> inventoryMap = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, inv -> inv));
        items.forEach(item -> {
            Inventory inventory = inventoryMap.get(item.productId());
            if (inventory != null) {
                inventory.setLockedStock(inventory.getLockedStock() + item.quantity());
                inventoryRepository.save(inventory);
            }
        });
    }

    private void releaseLockedStock(InventoryApplication application) {
        if (application.getType() != InventoryType.OUT) {
            return;
        }
        application.getItems().forEach(item -> {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new BusinessException("Inventory not found"));
            inventory.setLockedStock(Math.max(0, inventory.getLockedStock() - item.getQuantity()));
            inventoryRepository.save(inventory);
        });
    }

    private void applyExecution(InventoryType type, Map<Long, Inventory> inventoryMap, Map<Long, InventoryItem> itemMap,
            InventoryExecutionItem executionItem) {
        Inventory inventory = inventoryMap.get(executionItem.productId());
        if (inventory == null) {
            throw new BusinessException("Inventory not found for product " + executionItem.productId());
        }
        InventoryItem item = itemMap.get(executionItem.productId());
        if (item == null) {
            throw new BusinessException("Application item not found for product " + executionItem.productId());
        }
        item.setActualQuantity(executionItem.actualQuantity());
        if (type == InventoryType.IN) {
            inventory.setCurrentStock(inventory.getCurrentStock() + executionItem.actualQuantity());
        } else {
            inventory.setLockedStock(Math.max(0, inventory.getLockedStock() - item.getQuantity()));
            inventory.setCurrentStock(inventory.getCurrentStock() - executionItem.actualQuantity());
            if (inventory.getCurrentStock() < 0) {
                throw new BusinessException("Inventory cannot be negative for product " + executionItem.productId());
            }
        }
        inventoryRepository.save(inventory);
    }

    private InventoryItem toInventoryItem(InventoryItemRequest request) {
        InventoryItem item = new InventoryItem();
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        return item;
    }

    private InventoryApplicationResponse toResponse(InventoryApplication entity) {
        List<InventoryItemResponse> items = entity.getItems().stream()
                .map(item -> new InventoryItemResponse(item.getId(), item.getProductId(), item.getQuantity(),
                        item.getActualQuantity()))
                .toList();
        String applicantName = entity.getApplicant() != null ? entity.getApplicant().getRealName() : null;
        return new InventoryApplicationResponse(
                entity.getId(),
                entity.getType(),
                entity.getStatus(),
                applicantName,
                entity.getReason(),
                entity.getCreatedAt(),
                items);
    }
}
