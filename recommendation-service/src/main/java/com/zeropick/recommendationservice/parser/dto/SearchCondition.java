package com.zeropick.recommendationservice.parser.dto;

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
public class SearchCondition {
    private String category;
    /** 제외 감미료 — 상품 DB 정식명 목록 (없으면 null) */
    private List<String> sweetenerExcludes;
    private String allergenExclude;
    private BigDecimal sugarMax;
    private BigDecimal kcalMin;
    private BigDecimal kcalMax;
    private Integer maxPrice;
    private String query;

    /** product-service 쿼리 파라미터용 (쉼표 구분). 없으면 null */
    public String sweetenerExcludeParam() {
        return (sweetenerExcludes == null || sweetenerExcludes.isEmpty()) ? null : String.join(",", sweetenerExcludes);
    }

    /** 상품의 감미료 목록에 제외 감미료가 하나라도 있으면 true */
    public boolean containsExcludedSweetener(List<String> productSweeteners) {
        if (sweetenerExcludes == null || productSweeteners == null) return false;
        return productSweeteners.stream().anyMatch(sweetenerExcludes::contains);
    }
}
