package com.zeropick.commerceservice.controller;

import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class BehaviorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaTemplate<String, GenericRecord> kafkaTemplate;

    @Test
    void acceptsProductViewedAndPublishesKafkaEvent() throws Exception {
        mockMvc.perform(post("/commerce-service/behaviors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 1,
                                  "productId": 10,
                                  "eventType": "PRODUCT_VIEWED",
                                  "category": "간식/디저트",
                                  "occurredAt": "2026-08-19T01:02:03Z"
                                }
                                """))
                .andExpect(status().isAccepted());

        ArgumentCaptor<GenericRecord> recordCaptor = ArgumentCaptor.forClass(GenericRecord.class);
        verify(kafkaTemplate).send(eq("product-viewed"), eq("1"), recordCaptor.capture());

        GenericRecord record = recordCaptor.getValue();
        assertThat(record.get("memberId")).isEqualTo(1L);
        assertThat(record.get("productId")).isEqualTo(10L);
        assertThat(record.get("category").toString()).isEqualTo("간식/디저트");
        assertThat(record.get("occurredAt")).isEqualTo(1787101323000L);
    }

    @Test
    void rejectsUnsupportedBehaviorType() throws Exception {
        mockMvc.perform(post("/commerce-service/behaviors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 1,
                                  "productId": 10,
                                  "eventType": "CART_ADDED",
                                  "category": "간식/디저트"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/commerce-service/behaviors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "eventType": "PRODUCT_VIEWED" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
