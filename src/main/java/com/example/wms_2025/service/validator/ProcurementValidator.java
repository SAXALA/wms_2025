package com.example.wms_2025.service.validator;

import com.example.wms_2025.dto.procurement.CreateProcurementRequest;
import com.example.wms_2025.exception.BusinessException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProcurementValidator {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");

    public void validateCreation(CreateProcurementRequest request) {
        if (!StringUtils.hasText(request.title())) {
            throw new BusinessException("Title must not be blank");
        }
        if (request.totalAmount() == null || request.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Total amount must be positive");
        }
        if (request.totalAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException("Total amount exceeds configured limit");
        }
        request.items().forEach(item -> {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BusinessException("Quantity must be positive");
            }
        });
    }
}
