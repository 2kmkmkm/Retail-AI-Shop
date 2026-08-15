package com.zeropick.recommendationservice.dto;

import com.zeropick.recommendationservice.domain.RecoResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoResponse {

    private Long productId;
    private Integer rankNo;
    private BigDecimal score;
    private String reason;

    public static RecoResponse from(RecoResult result) {
        return RecoResponse.builder()
                .productId(result.getProductId())
                .rankNo(result.getRankNo())
                .score(result.getScore())
                .reason(result.getReason())
                .build();
    }
}