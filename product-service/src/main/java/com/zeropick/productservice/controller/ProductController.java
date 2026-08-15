package com.zeropick.productservice.controller;

import com.zeropick.productservice.dto.ProductResponse;
import com.zeropick.productservice.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/product-service/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sweetenerExclude,
            @RequestParam(required = false) String allergenExclude,
            @RequestParam(required = false) BigDecimal sugarMax,
            @RequestParam(required = false) BigDecimal kcalMin,
            @RequestParam(required = false) BigDecimal kcalMax,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort
    ) {
        return productService.findProducts(
                category, sweetenerExclude, allergenExclude,
                sugarMax, kcalMin, kcalMax, q, sort
        );
    }
}
