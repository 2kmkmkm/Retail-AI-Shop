package com.zeropick.commerceservice.event;

import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OrderCompletedEventListener {

    private static final String TOPIC = "order-completed";
    private static final Schema SCHEMA = loadSchema();

    private final KafkaTemplate<String, GenericRecord> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderCompletedEvent event) {
        GenericRecord record = new GenericRecordBuilder(SCHEMA)
                .set("memberId", event.memberId())
                .set("productId", event.productId())
                .set("category", event.category())
                .set("qty", event.qty())
                .set("unitPrice", event.unitPrice())
                .set("orderNo", event.orderNo())
                .set("paymentMethod", event.paymentMethod())
                .set("occurredAt", event.occurredAt().toEpochMilli())
                .build();

        kafkaTemplate.send(TOPIC, event.memberId().toString(), record);
    }

    private static Schema loadSchema() {
        try {
            return new Schema.Parser().parse(
                    new ClassPathResource("avro/order-completed.avsc").getInputStream()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("order-completed Avro 스키마를 읽을 수 없습니다.", exception);
        }
    }
}
