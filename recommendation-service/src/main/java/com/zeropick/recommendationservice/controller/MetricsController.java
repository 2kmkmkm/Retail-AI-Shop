package com.zeropick.recommendationservice.controller;

import com.zeropick.recommendationservice.dto.ClickRequest;
import com.zeropick.recommendationservice.dto.MetricsResponse;
import com.zeropick.recommendationservice.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommendation-service")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    // 추천 클릭 기록 수집 (POST /recommendation-service/click)
    @PostMapping("/click")
    public ResponseEntity<Void> recordClick(@RequestBody ClickRequest request) {
        metricsService.recordClick(request);
        return ResponseEntity.ok().build();
    }

    // 성과 지표 조회 (GET /recommendation-service/metrics)
    @GetMapping("/metrics")
    public ResponseEntity<MetricsResponse> getMetrics() {
        MetricsResponse response = metricsService.getMetrics();
        return ResponseEntity.ok(response);
    }
}