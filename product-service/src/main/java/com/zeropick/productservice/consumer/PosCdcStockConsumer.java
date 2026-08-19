package com.zeropick.productservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeropick.productservice.domain.Product;
import com.zeropick.productservice.exception.OutOfStockException;
import com.zeropick.productservice.exception.ProductNotFoundException;
import com.zeropick.productservice.service.StockService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * StockService 호출하여 오프라인 구매(OFFLINE_PURCHASE) 발생 시 온라인 재고 실시간 차감
 */

@Component
public class PosCdcStockConsumer {

    private static final Logger log = LoggerFactory.getLogger(PosCdcStockConsumer.class);

    private final StockService stockService;
    private final ObjectMapper objectMapper;

    public PosCdcStockConsumer(StockService stockService) {
        this.stockService = stockService;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(
            topics = "pos.pos_db.pos_stock_logs",
            groupId = "${spring.kafka.consumer.group-id:product-service}",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumePosStockSync(ConsumerRecord<String, String> record) {
        String payload = record.value();
        if (payload == null || payload.isBlank()) {
            return;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);

            // 1. Debezium Envelope 구조 대응
            JsonNode eventNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;
            JsonNode afterNode = eventNode.has("after") ? eventNode.get("after") : eventNode;

            if (afterNode == null || afterNode.isNull()) {
                return;
            }

            Long productId = afterNode.hasNonNull("product_id") ? afterNode.get("product_id").asLong() : null;
            Integer changedQty = afterNode.hasNonNull("changed_qty") ? afterNode.get("changed_qty").asInt() : 0;
            String eventType = afterNode.hasNonNull("event_type") ? afterNode.get("event_type").asText() : "";
            String storeId = afterNode.hasNonNull("store_id") ? afterNode.get("store_id").asText() : "UNKNOWN";

            // 2. 오프라인 구매(OFFLINE_PURCHASE) 발생 시 온라인 재고 실시간 차감
            if (productId != null && changedQty > 0 && "OFFLINE_PURCHASE".equals(eventType)) {
                Product updatedProduct = stockService.deduct(productId, changedQty);
                log.info("[POS 실시간 재고 동기화] storeId={}, productId={}, 차감수량={}, 잔여재고={}",
                        storeId, productId, changedQty, updatedProduct.getStock());
            }

        } catch (OutOfStockException e) {
            log.warn("[POS 재고 차감 경고] 온라인 재고 부족으로 차감 실패: {}", e.getMessage());
        } catch (ProductNotFoundException e) {
            log.warn("[POS 재고 차감 경고] 존재하지 않는 상품 ID: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[POS 재고 동기화 파싱 에러] payload={}, error={}", payload, e.getMessage(), e);
        }
    }
}