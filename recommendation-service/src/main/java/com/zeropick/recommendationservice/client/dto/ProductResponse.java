package com.zeropick.recommendationservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String brand;
    private String category;
    private Integer price;
    private String imageUrl;
    private Integer stock;
    private String claimType;

    // 영양성분
    private BigDecimal kcal;
    private BigDecimal sugarG;
    private BigDecimal carbG;
    private BigDecimal proteinG;
    private BigDecimal fatG;
    private BigDecimal sodiumMg;
    private BigDecimal servingSize;
    private String servingUnit;
    private String nutritionFactsUrl;

    // 조인 데이터 (감미료 / 알레르기 목록)
    private List<String> sweeteners; // 예: ["에리스리톨", "스테비아"]
    private List<String> allergens;   // 예: ["우유", "대두"]
}