package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.Product;
import com.example.wms_2025.dto.product.ProductResponse;
import com.example.wms_2025.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> listAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public Map<Long, ProductResponse> findAsMap(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .map(this::toResponse)
                .collect(Collectors.toMap(ProductResponse::id, product -> product));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getSku(), product.getUnit(),
                product.getPrice());
    }
}
