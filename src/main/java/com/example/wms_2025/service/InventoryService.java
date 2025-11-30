package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.ApprovalFlow;
import com.example.wms_2025.domain.entity.Inventory;
import com.example.wms_2025.domain.entity.InventoryApplication;
import com.example.wms_2025.domain.entity.InventoryItem;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.domain.entity.WarehouseLocation;
import com.example.wms_2025.domain.enums.BusinessType;
import com.example.wms_2025.domain.enums.InventoryApplicationStatus;
import com.example.wms_2025.domain.enums.InventoryType;
import com.example.wms_2025.domain.enums.RoleCode;
import com.example.wms_2025.dto.inventory.ApproveInventoryRequest;
import com.example.wms_2025.dto.inventory.CreateInventoryApplicationRequest;
import com.example.wms_2025.dto.inventory.ExecuteInventoryRequest;
import com.example.wms_2025.dto.inventory.InventoryApplicationResponse;
import com.example.wms_2025.dto.inventory.InventoryExecutionItem;
import com.example.wms_2025.dto.inventory.InventoryItemRequest;
import com.example.wms_2025.dto.inventory.InventoryItemResponse;
import com.example.wms_2025.dto.inventory.InventoryStockResponse;
import com.example.wms_2025.dto.product.ProductResponse;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.InventoryApplicationRepository;
import com.example.wms_2025.repository.InventoryRepository;
import com.example.wms_2025.repository.WarehouseLocationRepository;
import com.example.wms_2025.service.validator.InventoryValidator;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
public class InventoryService {

    private final InventoryApplicationRepository inventoryApplicationRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final UserService userService;
    private final WorkflowService workflowService;
    private final InventoryValidator inventoryValidator;
    private final ProductService productService;
    private final OperationLogService operationLogService;

    private static final String MODULE_INVENTORY = "库存管理";

    @Transactional
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public InventoryApplicationResponse createApplication(InventoryType type,
            CreateInventoryApplicationRequest request) {
        if (request.type() != type) {
            throw new BusinessException("Request type mismatch with endpoint");
        }
        User applicant = userService.getCurrentUser(RoleCode.OPERATOR, RoleCode.PURCHASER, RoleCode.ADMIN);
        inventoryValidator.validateCreateRequest(request);

        InventoryApplication application = new InventoryApplication();
        application.setType(type);
        application.setApplicant(applicant);
        application.setReason(request.reason());
        application.setStatus(InventoryApplicationStatus.PENDING_APPROVAL);
        request.items().forEach(itemRequest -> {
            ensureInventoryExists(itemRequest.productId());
            application.addItem(toInventoryItem(itemRequest));
        });

        User manager = userService.getManager();
        ApprovalFlow flow = workflowService.startFlow(applicant,
                type == InventoryType.IN ? BusinessType.INVENTORY_IN : BusinessType.INVENTORY_OUT,
                manager);
        application.setApprovalFlow(flow);

        lockStockIfNeeded(type, request.items());

        InventoryApplication saved = inventoryApplicationRepository.save(application);
        logOperation(type == InventoryType.IN ? "SUBMIT_INBOUND" : "SUBMIT_OUTBOUND",
                type == InventoryType.IN ? "提交入库申请" : "提交出库申请",
                saved);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public InventoryApplicationResponse approve(Long applicationId, ApproveInventoryRequest request) {
        Objects.requireNonNull(applicationId, "Application id is required");
        InventoryApplication application = inventoryApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("Inventory application not found"));
        if (application.getStatus() != InventoryApplicationStatus.PENDING_APPROVAL) {
            throw new BusinessException("Application already processed");
        }
        User approver = userService.getCurrentUser(RoleCode.MANAGER, RoleCode.ADMIN);
        workflowService.recordDecision(application.getApprovalFlow().getId(), approver, request.approved(),
                request.comment());
        if (!request.approved()) {
            application.setStatus(InventoryApplicationStatus.REJECTED);
            InventoryApplication saved = inventoryApplicationRepository.save(application);
            if (application.getType() == InventoryType.OUT) {
                releaseLockedStock(application);
            }
            logOperation(application.getType() == InventoryType.IN ? "REJECT_INBOUND" : "REJECT_OUTBOUND",
                    (application.getType() == InventoryType.IN ? "驳回入库申请" : "驳回出库申请"), saved);
            return toResponse(saved);
        }

        if (application.getType() == InventoryType.IN) {
            completeInbound(application);
        } else {
            completeOutbound(application);
        }

        InventoryApplication saved = inventoryApplicationRepository.save(application);
        logOperation(application.getType() == InventoryType.IN ? "APPROVE_INBOUND" : "APPROVE_OUTBOUND",
                (application.getType() == InventoryType.IN ? "审批通过入库申请" : "审批通过出库申请"), saved);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER','ADMIN')")
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

        Set<Long> locationIds = new HashSet<>();
        request.items().stream()
                .map(InventoryExecutionItem::locationId)
                .filter(Objects::nonNull)
                .forEach(locationIds::add);
        application.getItems().stream()
                .map(InventoryItem::getLocationId)
                .filter(Objects::nonNull)
                .forEach(locationIds::add);
        Map<Long, WarehouseLocation> locationMap = loadLocations(locationIds);

        application.setStatus(InventoryApplicationStatus.EXECUTING);
        request.items()
                .forEach(itemRequest -> applyExecution(application.getType(), inventoryMap, itemMap, itemRequest,
                        locationMap));
        application.setStatus(InventoryApplicationStatus.COMPLETED);
        InventoryApplication saved = inventoryApplicationRepository.save(application);
        logOperation(application.getType() == InventoryType.IN ? "EXECUTE_INBOUND" : "EXECUTE_OUTBOUND",
                (application.getType() == InventoryType.IN ? "执行入库申请" : "执行出库申请"), saved);
        return toResponse(saved);
    }

    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER','ADMIN')")
    public List<InventoryApplicationResponse> listByType(InventoryType type,
            List<InventoryApplicationStatus> statuses) {
        return inventoryApplicationRepository.findByTypeAndStatusIn(type, statuses).stream()
                .map(this::toResponse)
                .toList();
    }

    private void logOperation(String action, String prefix, InventoryApplication application) {
        String identifier = application.getId() != null ? "#" + application.getId() : "";
        operationLogService.record(MODULE_INVENTORY, action, prefix + identifier);
    }

    private void lockStockIfNeeded(InventoryType type, List<InventoryItemRequest> items) {
        if (type != InventoryType.OUT) {
            return;
        }
        items.forEach(item -> {
            Inventory inventory = ensureInventoryExists(item.productId());
            inventory.setLockedStock(inventory.getLockedStock() + item.quantity());
            inventoryRepository.save(inventory);
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

    private void applyExecution(InventoryType type,
            Map<Long, Inventory> inventoryMap,
            Map<Long, InventoryItem> itemMap,
            InventoryExecutionItem executionItem,
            Map<Long, WarehouseLocation> locationMap) {
        Inventory inventory = inventoryMap.get(executionItem.productId());
        if (inventory == null) {
            throw new BusinessException("Inventory not found for product " + executionItem.productId());
        }
        InventoryItem item = itemMap.get(executionItem.productId());
        if (item == null) {
            throw new BusinessException("Application item not found for product " + executionItem.productId());
        }
        item.setActualQuantity(executionItem.actualQuantity());
        Long targetLocationId = executionItem.locationId() != null ? executionItem.locationId() : item.getLocationId();
        WarehouseLocation targetLocation = null;
        if (targetLocationId != null) {
            targetLocation = locationMap.get(targetLocationId);
            if (targetLocation == null) {
                throw new BusinessException("Warehouse location not found: " + targetLocationId);
            }
            item.setLocationId(targetLocationId);
        }
        if (type == InventoryType.IN) {
            inventory.setCurrentStock(inventory.getCurrentStock() + executionItem.actualQuantity());
            if (targetLocation != null) {
                inventory.setLocation(targetLocation);
            }
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
        item.setLocationId(request.locationId());
        return item;
    }

    private InventoryApplicationResponse toResponse(InventoryApplication entity) {
        Set<Long> locationIds = entity.getItems().stream()
                .map(InventoryItem::getLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WarehouseLocation> locationMap = loadLocations(locationIds);
        List<InventoryItemResponse> items = entity.getItems().stream()
                .map(item -> {
                    WarehouseLocation location = item.getLocationId() != null
                            ? locationMap.get(item.getLocationId())
                            : null;
                    return new InventoryItemResponse(item.getId(),
                            item.getProductId(),
                            item.getQuantity(),
                            item.getActualQuantity(),
                            item.getLocationId(),
                            location != null ? location.getCode() : null,
                            location != null ? location.getName() : null);
                })
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

    public List<InventoryStockResponse> listStocks() {
        List<Inventory> inventories = inventoryRepository.findAll();
        Map<Long, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, inv -> inv, (a, b) -> a, HashMap::new));

        List<ProductResponse> products = productService.listAll();
        List<InventoryStockResponse> responses = new ArrayList<>();

        for (ProductResponse product : products) {
            Inventory inventory = inventoryMap.get(product.id());
            int current = inventory != null ? inventory.getCurrentStock() : 0;
            int safety = inventory != null ? inventory.getSafetyStock() : 0;
            int locked = inventory != null ? inventory.getLockedStock() : 0;
            WarehouseLocation location = inventory != null ? inventory.getLocation() : null;
            responses.add(new InventoryStockResponse(product.id(),
                    product.sku(),
                    product.name(),
                    product.unit(),
                    current,
                    safety,
                    locked,
                    location != null ? location.getId() : null,
                    location != null ? location.getCode() : null,
                    location != null ? location.getName() : null));
        }

        Set<Long> productIds = products.stream().map(ProductResponse::id).collect(Collectors.toSet());
        inventoryMap.values().stream()
                .filter(inv -> !productIds.contains(inv.getProductId()))
                .forEach(inv -> {
                    WarehouseLocation location = inv.getLocation();
                    responses.add(new InventoryStockResponse(inv.getProductId(),
                            "SKU-" + inv.getProductId(),
                            "商品" + inv.getProductId(),
                            null,
                            inv.getCurrentStock(),
                            inv.getSafetyStock(),
                            inv.getLockedStock(),
                            location != null ? location.getId() : null,
                            location != null ? location.getCode() : null,
                            location != null ? location.getName() : null));
                });

        return responses;
    }

    private void completeInbound(InventoryApplication application) {
        application.setStatus(InventoryApplicationStatus.COMPLETED);
        Set<Long> locationIds = application.getItems().stream()
                .map(InventoryItem::getLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WarehouseLocation> locationMap = loadLocations(locationIds);
        application.getItems().forEach(item -> {
            Inventory inventory = ensureInventoryExists(item.getProductId());
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            item.setActualQuantity(quantity);
            inventory.setCurrentStock(inventory.getCurrentStock() + quantity);
            if (item.getLocationId() != null) {
                WarehouseLocation location = locationMap.get(item.getLocationId());
                if (location == null) {
                    throw new BusinessException("Warehouse location not found: " + item.getLocationId());
                }
                inventory.setLocation(location);
            }
            inventoryRepository.save(inventory);
        });
    }

    private void completeOutbound(InventoryApplication application) {
        application.setStatus(InventoryApplicationStatus.COMPLETED);
        Set<Long> locationIds = application.getItems().stream()
                .map(InventoryItem::getLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WarehouseLocation> locationMap = loadLocations(locationIds);
        application.getItems().forEach(item -> {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new BusinessException("Inventory not found"));
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            item.setActualQuantity(quantity);
            int remainingLocked = Math.max(0, inventory.getLockedStock() - quantity);
            inventory.setLockedStock(remainingLocked);
            int remainingStock = inventory.getCurrentStock() - quantity;
            if (remainingStock < 0) {
                throw new BusinessException("Insufficient stock for product " + item.getProductId());
            }
            inventory.setCurrentStock(remainingStock);
            if (item.getLocationId() != null) {
                WarehouseLocation location = locationMap.get(item.getLocationId());
                if (location == null) {
                    throw new BusinessException("Warehouse location not found: " + item.getLocationId());
                }
                if (Boolean.FALSE.equals(location.getActive())) {
                    throw new BusinessException("Warehouse location " + location.getCode() + " 已停用");
                }
            }
            inventoryRepository.save(inventory);
        });
    }

    private Inventory ensureInventoryExists(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Inventory inventory = new Inventory();
                    inventory.setProductId(productId);
                    inventory.setCurrentStock(0);
                    inventory.setSafetyStock(50);
                    inventory.setLockedStock(0);
                    return inventoryRepository.save(inventory);
                });
    }

    private Map<Long, WarehouseLocation> loadLocations(Set<Long> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            return Map.of();
        }
        return warehouseLocationRepository.findAllById(locationIds).stream()
                .collect(Collectors.toMap(WarehouseLocation::getId, location -> location));
    }
}
