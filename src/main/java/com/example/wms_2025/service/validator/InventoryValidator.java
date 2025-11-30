package com.example.wms_2025.service.validator;

import com.example.wms_2025.domain.entity.Inventory;
import com.example.wms_2025.domain.entity.WarehouseLocation;
import com.example.wms_2025.domain.enums.InventoryType;
import com.example.wms_2025.dto.inventory.CreateInventoryApplicationRequest;
import com.example.wms_2025.dto.inventory.ExecuteInventoryRequest;
import com.example.wms_2025.dto.inventory.InventoryItemRequest;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.InventoryRepository;
import com.example.wms_2025.repository.WarehouseLocationRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryValidator {

    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;

    public void validateCreateRequest(CreateInventoryApplicationRequest request) {
        Map<Long, Inventory> inventoryMap = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));
        Map<Long, WarehouseLocation> locationMap = loadLocations(
                request.items().stream().map(InventoryItemRequest::locationId));
        request.items().forEach(item -> {
            Inventory inventory = inventoryMap.get(item.productId());
            if (inventory == null) {
                if (request.type() == InventoryType.IN) {
                    return;
                }
                throw new BusinessException("Product " + item.productId() + " does not exist in inventory");
            }
            if (request.type() == InventoryType.OUT && item.quantity() > availableQuantity(inventory)) {
                throw new BusinessException("Insufficient stock for product " + item.productId());
            }
            WarehouseLocation location = resolveLocation(locationMap, item.locationId());
            if (location != null && Boolean.FALSE.equals(location.getActive())) {
                throw new BusinessException("Warehouse location " + location.getCode() + " 已停用");
            }
        });
    }

    public void validateExecution(ExecuteInventoryRequest request, InventoryType type,
            Map<Long, Inventory> inventoryMap) {
        Map<Long, WarehouseLocation> locationMap = loadLocations(
                request.items().stream().map(item -> item.locationId()));
        request.items().forEach(item -> {
            Inventory inventory = inventoryMap.get(item.productId());
            if (inventory == null) {
                throw new BusinessException("Product " + item.productId() + " does not exist in inventory");
            }
            if (type == InventoryType.OUT && item.actualQuantity() > availableQuantity(inventory)) {
                throw new BusinessException("Insufficient stock for product " + item.productId());
            }
            WarehouseLocation location = resolveLocation(locationMap, item.locationId());
            if (location != null && Boolean.FALSE.equals(location.getActive())) {
                throw new BusinessException("Warehouse location " + location.getCode() + " 已停用");
            }
        });
    }

    private int availableQuantity(Inventory inventory) {
        return inventory.getCurrentStock() - inventory.getLockedStock();
    }

    private Map<Long, WarehouseLocation> loadLocations(Stream<Long> idStream) {
        List<Long> ids = idStream
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return warehouseLocationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(WarehouseLocation::getId, Function.identity()));
    }

    private WarehouseLocation resolveLocation(Map<Long, WarehouseLocation> locationMap, Long locationId) {
        if (locationId == null) {
            return null;
        }
        WarehouseLocation location = locationMap.get(locationId);
        if (location == null) {
            throw new BusinessException("Warehouse location not found: " + locationId);
        }
        return location;
    }
}
