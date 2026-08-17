package com.zeropick.recommendationservice.search.dto;

import com.zeropick.recommendationservice.client.dto.ProductResponse;
import com.zeropick.recommendationservice.parser.dto.SearchCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {
    private SearchCondition extractedCondition;
    private boolean usedFallback;
    private int totalCount;
    private List<ProductResponse> products;
}