package com.zeropick.recommendationservice.search;

import com.zeropick.recommendationservice.client.ProductServiceClient;
import com.zeropick.recommendationservice.client.dto.ProductResponse;
import com.zeropick.recommendationservice.llm.LlmQueryService;
import com.zeropick.recommendationservice.llm.dto.LlmParseResult;
import com.zeropick.recommendationservice.parser.dto.SearchCondition;
import com.zeropick.recommendationservice.search.dto.SearchRequest;
import com.zeropick.recommendationservice.search.dto.SearchResponse;
import com.zeropick.recommendationservice.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final LlmQueryService llmQueryService;
    private final ProductServiceClient productServiceClient;
    private final MetricsService metricsService;

    /**
     * POST /search 자연어 검색 처리 파이프라인 (FR-11)
     * 1) LlmQueryService로 질의 조건 추출 (CircuitBreaker 적용)
     * 2) Feign Client로 product-service 상품 검색 호출
     * 3) 2차 메모리 하드 필터링 (제외 감미료 위반율 0% 보장)
     * 4) 응답 반환 및 응답시간(ms) 로깅
     */
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        String query = request != null ? request.getQuery() : "";

        // 1. LLM / Fallback 질의 조건 파싱
        LlmParseResult parseResult = llmQueryService.extractCondition(query);
        SearchCondition condition = parseResult.getCondition();
        boolean usedFallback = parseResult.isUsedFallback();

        // 지표 카운트 누적
        metricsService.incrementChatRequest(usedFallback);

        // 2. product-service 상품 목록 조회
        List<ProductResponse> allProducts;
        try {
            allProducts = productServiceClient.getProducts(
                    condition.getCategory(),
                    condition.getSweetenerExclude(),
                    condition.getAllergenExclude(),
                    condition.getSugarMax(),
                    condition.getKcalMin(),
                    condition.getKcalMax(),
                    condition.getQuery(),
                    null
            );
            if (allProducts == null) {
                allProducts = Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("[product-service 검색 통신 오류] 상품 조회 실패: {}", e.getMessage());
            allProducts = Collections.emptyList();
        }

        // 3. 메모리 레벨 2차 하드 필터링 (품질 목표 0% 보장)
        List<ProductResponse> filteredProducts = allProducts.stream()
                .filter(p -> {
                    if (condition.getSweetenerExclude() == null) return true;
                    String exclude = condition.getSweetenerExclude();
                    return p.getSweeteners() == null || !p.getSweeteners().contains(exclude);
                })
                .filter(p -> {
                    if (condition.getAllergenExclude() == null) return true;
                    String allergen = condition.getAllergenExclude();
                    return p.getAllergens() == null || !p.getAllergens().contains(allergen);
                })
                .filter(p -> {
                    if (condition.getKcalMax() == null) return true;
                    return p.getKcal() != null && p.getKcal().compareTo(condition.getKcalMax()) <= 0;
                })
                .filter(p -> {
                    if (condition.getMaxPrice() == null) return true;
                    return p.getPrice() != null && p.getPrice() <= condition.getMaxPrice();
                })
                .filter(p -> {
                    if (condition.getSugarMax() == null) return true;
                    return p.getSugarG() != null && p.getSugarG().compareTo(condition.getSugarMax()) <= 0;
                })
                .toList();

        long duration = System.currentTimeMillis() - startTime;
        log.info("[자연어 검색 완료] query='{}', 결과건수={}, usedFallback={}, 소요시간={}ms",
                query, filteredProducts.size(), usedFallback, duration);

        return SearchResponse.builder()
                .extractedCondition(condition)
                .usedFallback(usedFallback)
                .totalCount(filteredProducts.size())
                .products(filteredProducts)
                .build();
    }
}