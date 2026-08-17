package com.zeropick.recommendationservice.client;

import com.zeropick.recommendationservice.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    /**
     * 상품 상세 단건 조회 (GET /products/{id})
     */
    @GetMapping("/products/{id}")
    ProductResponse getProductById(@PathVariable("id") Long id);

    /**
     * 상품 다건/비교 조회 (GET /products/compare?ids=1,2,3)
     * - 추천 결과나 챗봇 후보 상품들의 세부 정보(영양/감미료)를 한 번에 가져올 때 사용
     */
    @GetMapping("/products/compare")
    List<ProductResponse> getProductsByIds(@RequestParam("ids") List<Long> ids);

    /**
     * 전체/필터 상품 목록 조회 (GET /products)
     * - 챗봇/자연어 검색 후보군 추출 시 사용
     */
    @GetMapping("/products")
    List<ProductResponse> getProducts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sweetenerExclude", required = false) String sweetenerExclude,
            @RequestParam(value = "sugarMax", required = false) BigDecimal sugarMax,
            @RequestParam(value = "q", required = false) String query
    );
}