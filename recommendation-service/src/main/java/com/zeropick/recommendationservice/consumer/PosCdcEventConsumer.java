package com.zeropick.recommendationservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeropick.recommendationservice.domain.BehaviorLog;
import com.zeropick.recommendationservice.repository.BehaviorLogRepository;
import com.zeropick.recommendationservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PosCdcEventConsumer {

    private final BehaviorLogRepository behaviorLogRepository;
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @KafkaListener(
            topics = "pos.pos_db.pos_stock_logs",
            groupId = "${spring.kafka.consumer.group-id:recommendation-service}",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumePosCdcEvent(ConsumerRecord<String, String> record) {
        String payload = record.value();

        if (payload == null || payload.isBlank()) {
            log.warn("[CDC] 빈 메시지(Tombstone) 수신: topic={}", record.topic());
            return;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);

            // 1. Debezium Envelope 구조 대응 (payload 노드가 있으면 내부로 진입)
            JsonNode eventNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;

            // 2. after 노드 추출
            JsonNode afterNode = eventNode.has("after") ? eventNode.get("after") : eventNode;
            if (afterNode == null || afterNode.isNull()) {
                log.info("[CDC 스킵] after 데이터 없음 (DELETE 등)");
                return;
            }

            // 3. 가상 POS DB 컬럼 추출 (null-safe)
            Long memberId = afterNode.hasNonNull("member_id") ? afterNode.get("member_id").asLong() : null;
            Long productId = afterNode.hasNonNull("product_id") ? afterNode.get("product_id").asLong() : null;
            String category = afterNode.hasNonNull("category") ? afterNode.get("category").asText() : "오프라인";
            Integer changedQty = afterNode.hasNonNull("changed_qty") ? afterNode.get("changed_qty").asInt() : 1;
            String eventType = afterNode.hasNonNull("event_type") ? afterNode.get("event_type").asText() : "OFFLINE_PURCHASE";
            String storeId = afterNode.hasNonNull("store_id") ? afterNode.get("store_id").asText() : "UNKNOWN";

            if (memberId == null) {
                log.warn("[CDC 스킵] memberId가 존재하지 않습니다: {}", afterNode);
                return;
            }

            // 4. BehaviorLog 엔티티 생성 및 DB 적재
            BehaviorLog behaviorLog = BehaviorLog.builder()
                    .memberId(memberId)
                    .productId(productId)
                    .category(category)
                    .eventType(eventType)
                    .qty(changedQty)
                    .orderNo("POS-" + storeId)
                    .paymentMethod("OFFLINE_POS")
                    .occurredAt(LocalDateTime.now())
                    .build();

            behaviorLogRepository.save(behaviorLog);

            log.info("[CDC 적재 완료] memberId={}, productId={}, storeId={}, qty={}",
                    memberId, productId, storeId, changedQty);

            // 5. 오프라인 구매 행동 즉시 추천 점수 재계산
            recommendationService.calculateAndGetRecommendations(memberId);
            log.info("[CDC 연계 추천 갱신 성공] memberId={}", memberId);

        } catch (Exception e) {
            log.error("[CDC 역직렬화 및 파싱 처리 실패] payload={}, error={}", payload, e.getMessage(), e);
        }
    }
}