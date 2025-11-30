package com.example.wms_2025.controller;

import com.example.wms_2025.dto.common.ApiResponse;
import com.example.wms_2025.dto.product.ProductResponse;
import com.example.wms_2025.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listProducts() {
        List<ProductResponse> products = productService.listAll();
        return ResponseEntity.ok(ApiResponse.ok("Fetched products", products));
    }
}
