package com.zeropick.productservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        String brand,
        String category,
        Integer price,
        String imageUrl,
        Integer stock,
        String claimType,
        BigDecimal kcal,
        BigDecimal sugarG,
        BigDecimal carbG,
        List<String> sweeteners,
        List<String> allergens,
        BigDecimal proteinG,
        BigDecimal fatG,
        BigDecimal sodiumMg,
        BigDecimal servingSize,
        String servingUnit,
        String nutritionFactsUrl,
        List<SweetenerAmount> sweetenerAmounts,
        String verificationSource,
        LocalDateTime createdAt
) {
    public record SweetenerAmount(String name, BigDecimal amountG) {
    }
}
