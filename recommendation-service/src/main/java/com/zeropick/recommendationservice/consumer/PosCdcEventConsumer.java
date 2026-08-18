package com.zeropick.recommendationservice.consumer;

import com.zeropick.recommendationservice.domain.BehaviorLog;
import com.zeropick.recommendationservice.repository.BehaviorLogRepository;
import com.zeropick.recommendationservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
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

    @Transactional
    @KafkaListener(
            topics = "pos.pos_db.pos_stock_logs",
            groupId = "${spring.kafka.consumer.group-id:recommendation-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePosCdcEvent(ConsumerRecord<String, GenericRecord> record) {
        GenericRecord value = record.value();

        if (value == null) {
            log.warn("[CDC] 빈 메시지(Tombstone) 수신: topic={}", record.topic());
            return;
        }

        try {
            // Debezium 이벤트의 연산 타입 ('c': Create/Insert, 'u': Update, 'd': Delete)
            String op = extractString(value, "op");
            log.info("[CDC 감지] op={}, topic={}", op, record.topic());

            // 1. INSERT 이벤트의 'after' 레코드 추출
            GenericRecord after = extractRecord(value, "after");
            if (after == null) {
                log.info("[CDC 스킵] after 데이터 없음 (DELETE 등)");
                return;
            }

            // 2. 가상 POS DB 테이블(pos_stock_logs) 컬럼 추출
            Long memberId = extractLong(after, "member_id");
            Long productId = extractLong(after, "product_id");
            String category = extractString(after, "category");
            Integer changedQty = extractInteger(after, "changed_qty");
            String eventType = extractString(after, "event_type"); // OFFLINE_PURCHASE
            String storeId = extractString(after, "store_id");

            // 3. BehaviorLog 엔티티 생성 및 DB 적재
            BehaviorLog behaviorLog = BehaviorLog.builder()
                    .memberId(memberId)
                    .productId(productId)
                    .category(category != null ? category : "오프라인")
                    .eventType(eventType != null ? eventType : "OFFLINE_PURCHASE")
                    .qty(changedQty)
                    .orderNo("POS-" + storeId)
                    .paymentMethod("OFFLINE_POS")
                    .occurredAt(LocalDateTime.now())
                    .build();

            behaviorLogRepository.save(behaviorLog);

            log.info("[CDC 적재 완료] memberId={}, productId={}, storeId={}, qty={}",
                    memberId, productId, storeId, changedQty);

            // 4. 오프라인 구매 행동 즉시 추천 점수 재계산
            if (memberId != null) {
                recommendationService.calculateAndGetRecommendations(memberId);
                log.info("[CDC 연계 추천 갱신 성공] memberId={}", memberId);
            }

        } catch (Exception e) {
            log.error("[CDC 처리 실패] error={}", e.getMessage(), e);
        }
    }

    // --- Null-Safe GenericRecord 헬퍼 메서드 ---
    private GenericRecord extractRecord(GenericRecord record, String fieldName) {
        if (record.getSchema().getField(fieldName) == null) return null;
        Object val = record.get(fieldName);
        return (val instanceof GenericRecord gr) ? gr : null;
    }

    private Long extractLong(GenericRecord record, String fieldName) {
        if (record.getSchema().getField(fieldName) == null) return null;
        Object val = record.get(fieldName);
        return val != null ? ((Number) val).longValue() : null;
    }

    private Integer extractInteger(GenericRecord record, String fieldName) {
        if (record.getSchema().getField(fieldName) == null) return null;
        Object val = record.get(fieldName);
        return val != null ? ((Number) val).intValue() : null;
    }

    private String extractString(GenericRecord record, String fieldName) {
        if (record.getSchema().getField(fieldName) == null) return null;
        Object val = record.get(fieldName);
        return val != null ? val.toString() : null;
    }
}