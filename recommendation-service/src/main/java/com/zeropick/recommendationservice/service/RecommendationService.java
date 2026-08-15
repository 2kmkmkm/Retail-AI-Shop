package com.zeropick.recommendationservice.service;

import com.zeropick.recommendationservice.client.ProductServiceClient;
import com.zeropick.recommendationservice.client.dto.ProductResponse;
import com.zeropick.recommendationservice.domain.BehaviorLog;
import com.zeropick.recommendationservice.domain.RecoResult;
import com.zeropick.recommendationservice.dto.RecoDetailResponse;
import com.zeropick.recommendationservice.dto.RecoResponse;
import com.zeropick.recommendationservice.repository.BehaviorLogRepository;
import com.zeropick.recommendationservice.repository.RecoResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final BehaviorLogRepository behaviorLogRepository;
    private final RecoResultRepository recoResultRepository;

    private final ProductServiceClient productServiceClient;

    /**
     * 추천 점수 계산 및 reco_result 갱신 후 상위 TOP 3 반환
     */
    @Transactional
    public List<RecoResponse> calculateAndGetRecommendations(Long memberId) {
        long startTime = System.currentTimeMillis();

        // 1. 회원의 모든 행동 로그 조회
        List<BehaviorLog> logs = behaviorLogRepository.findByMemberIdOrderByOccurredAtDesc(memberId);

        // 행동 로그가 없는 경우 (콜드 스타트) 기존 추천 결과가 있다면 반환, 없으면 빈 목록
        if (logs.isEmpty()) {
            return recoResultRepository.findByMemberIdOrderByRankNoAsc(memberId).stream()
                    .limit(3)
                    .map(RecoResponse::from)
                    .collect(Collectors.toList());
        }

        // 2. 상품별 점수 누적 합산 및 추천 사유 생성[cite: 1]
        Map<Long, Double> productScores = new HashMap<>();
        Map<Long, String> productReasons = new HashMap<>();

        for (BehaviorLog logItem : logs) {
            Long productId = logItem.getProductId();
            if (productId == null) continue;

            double weight = switch (logItem.getEventType()) {
                case "PRODUCT_VIEWED" -> 1.0;
                case "ORDER_COMPLETED" -> 50.0;
                case "CART_ADDED" -> 0.0;
                default -> 0.0;
            }; //[cite: 1]

            productScores.put(productId, productScores.getOrDefault(productId, 0.0) + weight);

            // 우선순위가 높은 행동을 추천 사유로 지정
            if ("ORDER_COMPLETED".equals(logItem.getEventType())) {
                productReasons.put(productId, "최근 구매한 상품 기반 추천");
            } else if (!productReasons.containsKey(productId)) {
                productReasons.put(productId, "최근 조회/관심 상품 기반 추천");
            }
        }

        // 3. 점수 내림차순 정렬 후 상위 3개(TOP 3)만 추출
        List<Map.Entry<Long, Double>> sortedList = productScores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3) // 상위 3개로 제한
                .toList();

        // 4. RecoResult 엔티티 리스트 생성 및 랭킹 부여 (1~3위)
        List<RecoResult> newResults = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, Double> entry : sortedList) {
            newResults.add(RecoResult.builder()
                    .memberId(memberId)
                    .productId(entry.getKey())
                    .rankNo(rank++)
                    .score(BigDecimal.valueOf(entry.getValue()))
                    .reason(productReasons.getOrDefault(entry.getKey(), "사용자 맞춤 추천"))
                    .build());
        }

        // 5. DB 갱신: 기존 reco_result 삭제 후 새 결과 일괄 저장
        recoResultRepository.deleteByMemberId(memberId);
        List<RecoResult> saved = recoResultRepository.saveAll(newResults);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[추천 계산 완료] memberId={}, 추천 상품 수={}, 소요시간={}ms", memberId, saved.size(), duration);

        // 6. DTO 변환 반환
        return saved.stream()
                .map(RecoResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 추천 결과에 product-service의 상품 상세 정보를 결합하여 반환 (Enrichment)
     */
    public List<RecoDetailResponse> getDetailedRecommendations(Long memberId) {
        // 1. 기존 추천 결과(점수/랭킹) 조회
        List<RecoResult> recoResults = recoResultRepository.findByMemberIdOrderByRankNoAsc(memberId);
        if (recoResults.isEmpty()) {
            calculateAndGetRecommendations(memberId);
            recoResults = recoResultRepository.findByMemberIdOrderByRankNoAsc(memberId);
        }

        if (recoResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 추천 상품 ID 목록 추출
        List<Long> productIds = recoResults.stream()
                .map(RecoResult::getProductId)
                .toList();

        // 3. OpenFeign으로 product-service 다건 조회 호출
        Map<Long, ProductResponse> productMap = new HashMap<>();
        try {
            List<ProductResponse> products = productServiceClient.getProductsByIds(productIds);
            for (ProductResponse p : products) {
                productMap.put(p.getId(), p);
            }
        } catch (Exception e) {
            log.error("[Feign 실패] product-service 호출 실패: {}", e.getMessage());
            // product-service 호출 실패 시에도 기본 점수 정보는 내려갈 수 있도록 fallback 처리
        }

        // 4. 상세 응답 매핑
        return recoResults.stream()
                .map(r -> RecoDetailResponse.of(r, productMap.get(r.getProductId())))
                .collect(Collectors.toList());
    }
}