package com.example.wms_2025.repository;

import com.example.wms_2025.domain.entity.WarehouseLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {
    Optional<WarehouseLocation> findByCodeIgnoreCase(String code);

    List<WarehouseLocation> findByActiveTrueOrderByCodeAsc();

    List<WarehouseLocation> findAllByOrderByCodeAsc();
}
