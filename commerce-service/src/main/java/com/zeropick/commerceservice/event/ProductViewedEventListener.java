package com.zeropick.commerceservice.event;

import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ProductViewedEventListener {

    private static final String TOPIC = "product-viewed";
    private static final Schema SCHEMA = loadSchema();

    private final KafkaTemplate<String, GenericRecord> kafkaTemplate;

    @EventListener
    public void publish(ProductViewedEvent event) {
        GenericRecord record = new GenericRecordBuilder(SCHEMA)
                .set("memberId", event.memberId())
                .set("productId", event.productId())
                .set("category", event.category())
                .set("occurredAt", event.occurredAt().toEpochMilli())
                .build();

        kafkaTemplate.send(TOPIC, event.memberId().toString(), record);
    }

    private static Schema loadSchema() {
        try {
            return new Schema.Parser().parse(
                    new ClassPathResource("avro/product-viewed.avsc").getInputStream()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("product-viewed Avro 스키마를 읽을 수 없습니다.", exception);
        }
    }
}
