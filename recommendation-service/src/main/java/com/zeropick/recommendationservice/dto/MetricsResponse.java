package com.zeropick.recommendationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricsResponse {
    private long totalImpressions;
    private long totalClicks;
    private double clickThroughRate;
    private long totalChatRequests;
    private long fallbackCount;
    private double fallbackRate;
}