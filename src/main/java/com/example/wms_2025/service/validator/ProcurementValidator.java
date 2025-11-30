package com.example.wms_2025.service.validator;

import com.example.wms_2025.dto.procurement.CreateProcurementRequest;
import com.example.wms_2025.dto.procurement.ProcurementItemRequest;
import com.example.wms_2025.dto.product.ProductResponse;
import com.example.wms_2025.exception.BusinessException;
import com.example.wms_2025.service.ProductService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ProcurementValidator {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ProductService productService;

    public ValidationResult validateCreation(CreateProcurementRequest request) {
        if (!StringUtils.hasText(request.title())) {
            throw new BusinessException("Title must not be blank");
        }
        if (request.totalAmount() == null || request.totalAmount().compareTo(ZERO) <= 0) {
            throw new BusinessException("Total amount must be positive");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException("At least one procurement item is required");
        }

        Set<Long> productIds = request.items().stream()
                .map(item -> {
                    if (item.productId() == null) {
                        throw new BusinessException("Product must be specified for each item");
                    }
                    return item.productId();
                })
                .collect(Collectors.toSet());
        Map<Long, ProductResponse> productMap = productService.findAsMap(productIds);
        if (productMap.size() != productIds.size()) {
            throw new BusinessException("Contains invalid product references");
        }

        BigDecimal calculatedTotal = ZERO;
        for (ProcurementItemRequest item : request.items()) {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BusinessException("Quantity must be positive");
            }
            ProductResponse product = productMap.get(item.productId());
            BigDecimal unitPrice = item.expectedPrice() != null ? item.expectedPrice() : product.price();
            if (unitPrice == null || unitPrice.compareTo(ZERO) < 0) {
                throw new BusinessException("Invalid price detected for selected products");
            }
            calculatedTotal = calculatedTotal.add(unitPrice.multiply(BigDecimal.valueOf(item.quantity())));
        }

        BigDecimal normalizedTotal = calculatedTotal.setScale(2, RoundingMode.HALF_UP);
        if (normalizedTotal.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException("Total amount exceeds configured limit");
        }

        BigDecimal declaredTotal = request.totalAmount().setScale(2, RoundingMode.HALF_UP);
        if (declaredTotal.compareTo(normalizedTotal) != 0) {
            throw new BusinessException("Total amount mismatch detected");
        }

        return new ValidationResult(normalizedTotal, productMap);
    }

    public record ValidationResult(BigDecimal totalAmount, Map<Long, ProductResponse> products) {
    }
}
