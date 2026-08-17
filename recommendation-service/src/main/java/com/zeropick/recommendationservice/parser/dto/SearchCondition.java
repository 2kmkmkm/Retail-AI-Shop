package com.zeropick.recommendationservice.parser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchCondition {
    private String category;
    private String sweetenerExclude;
    private String allergenExclude;
    private BigDecimal sugarMax;
    private BigDecimal kcalMin;
    private BigDecimal kcalMax;
    private Integer maxPrice;
    private String query;
}