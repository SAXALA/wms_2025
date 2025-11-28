package com.example.wms_2025.service.validator;

import com.example.wms_2025.domain.entity.Inventory;
import com.example.wms_2025.domain.enums.InventoryType;
import com.example.wms_2025.dto.inventory.CreateInventoryApplicationRequest;
import com.example.wms_2025.dto.inventory.ExecuteInventoryRequest;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.InventoryRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryValidator {

    private final InventoryRepository inventoryRepository;

    public void validateCreateRequest(CreateInventoryApplicationRequest request) {
        Map<Long, Inventory> inventoryMap = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));
        request.items().forEach(item -> {
            Inventory inventory = inventoryMap.get(item.productId());
            if (inventory == null) {
                throw new BusinessException("Product " + item.productId() + " does not exist in inventory");
            }
            if (request.type() == InventoryType.OUT && item.quantity() > availableQuantity(inventory)) {
                throw new BusinessException("Insufficient stock for product " + item.productId());
            }
        });
    }

    public void validateExecution(ExecuteInventoryRequest request, InventoryType type,
            Map<Long, Inventory> inventoryMap) {
        request.items().forEach(item -> {
            Inventory inventory = inventoryMap.get(item.productId());
            if (inventory == null) {
                throw new BusinessException("Product " + item.productId() + " does not exist in inventory");
            }
            if (type == InventoryType.OUT && item.actualQuantity() > availableQuantity(inventory)) {
                throw new BusinessException("Insufficient stock for product " + item.productId());
            }
        });
    }

    private int availableQuantity(Inventory inventory) {
        return inventory.getCurrentStock() - inventory.getLockedStock();
    }
}
