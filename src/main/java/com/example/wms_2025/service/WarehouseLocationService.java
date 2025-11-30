package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.WarehouseLocation;
import com.example.wms_2025.dto.warehouse.WarehouseLocationRequest;
import com.example.wms_2025.dto.warehouse.WarehouseLocationResponse;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.repository.WarehouseLocationRepository;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseLocationService {

    private final WarehouseLocationRepository warehouseLocationRepository;

    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER','ADMIN')")
    public List<WarehouseLocationResponse> listActive() {
        return warehouseLocationRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<WarehouseLocationResponse> listAll() {
        return warehouseLocationRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public WarehouseLocationResponse create(WarehouseLocationRequest request) {
        String code = normalizeCode(request.code());
        ensureCodeUnique(code, null);

        WarehouseLocation location = new WarehouseLocation();
        location.setCode(code);
        location.setName(request.name().trim());
        location.setDescription(safeTrim(request.description()));
        location.setActive(request.active() == null ? Boolean.TRUE : request.active());

        WarehouseLocation saved = warehouseLocationRepository.save(location);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public WarehouseLocationResponse update(Long id, WarehouseLocationRequest request) {
        WarehouseLocation location = warehouseLocationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Warehouse location not found"));
        String newCode = normalizeCode(request.code());
        ensureCodeUnique(newCode, id);

        location.setCode(newCode);
        location.setName(request.name().trim());
        location.setDescription(safeTrim(request.description()));
        if (request.active() != null) {
            location.setActive(request.active());
        }

        WarehouseLocation saved = warehouseLocationRepository.save(location);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public WarehouseLocationResponse toggleStatus(Long id, boolean active) {
        WarehouseLocation location = warehouseLocationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Warehouse location not found"));
        location.setActive(active);
        WarehouseLocation saved = warehouseLocationRepository.save(location);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public void delete(Long id) {
        WarehouseLocation location = warehouseLocationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Warehouse location not found"));
        try {
            warehouseLocationRepository.delete(location);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Unable to delete location that is referenced by inventory records");
        }
    }

    public List<WarehouseLocation> resolveLocations(Set<Long> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            return List.of();
        }
        return warehouseLocationRepository.findAllById(new HashSet<>(locationIds));
    }

    private WarehouseLocationResponse toResponse(WarehouseLocation entity) {
        return new WarehouseLocationResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new BusinessException("Location code is required");
        }
        return code.trim().toUpperCase();
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private void ensureCodeUnique(String code, Long currentId) {
        warehouseLocationRepository.findByCodeIgnoreCase(code)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new BusinessException("Location code already exists");
                });
    }

    public List<WarehouseLocationResponse> mapToResponses(List<WarehouseLocation> locations) {
        return locations.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
