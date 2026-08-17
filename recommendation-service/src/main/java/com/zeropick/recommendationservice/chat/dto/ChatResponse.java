package com.zeropick.recommendationservice.chat.dto;

import com.zeropick.recommendationservice.parser.dto.SearchCondition;
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
public class ChatResponse {
    private String reply;
    private SearchCondition extractedCondition;
    private boolean usedFallback;
    private List<ChatProductItem> products;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatProductItem {
        private Long productId;
        private String name;
        private String brand;
        private String category;
        private Integer price;
        private String imageUrl;
        private BigDecimal sugarG;
        private List<String> sweeteners;
        private String reason;
    }
}