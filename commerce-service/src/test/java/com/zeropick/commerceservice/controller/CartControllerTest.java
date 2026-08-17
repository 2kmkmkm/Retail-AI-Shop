package com.zeropick.commerceservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeropick.commerceservice.client.ProductCatalogService;
import com.zeropick.commerceservice.entity.Member;
import com.zeropick.commerceservice.repository.CartItemRepository;
import com.zeropick.commerceservice.repository.MemberRepository;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @MockitoBean
    private ProductCatalogService productCatalogService;

    @MockitoBean
    private KafkaTemplate<String, GenericRecord> kafkaTemplate;

    private Long memberId;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        memberRepository.deleteAll();
        Member member = memberRepository.save(Member.builder()
                .email("cart@example.com")
                .password("encoded-password")
                .name("장바구니 테스트")
                .build());
        memberId = member.getId();
        when(productCatalogService.getCategory(10L)).thenReturn("간식/디저트");
    }

    @Test
    void supportsCreateReadUpdateDeleteAndPublishesCartAdded() throws Exception {
        String firstResponse = mockMvc.perform(post("/commerce-service/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", memberId, "productId", 10, "qty", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.qty").value(2))
                .andReturn().getResponse().getContentAsString();

        long cartItemId = objectMapper.readTree(firstResponse).path("id").asLong();

        mockMvc.perform(post("/commerce-service/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", memberId, "productId", 10, "qty", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cartItemId))
                .andExpect(jsonPath("$.qty").value(3));

        assertThat(cartItemRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/commerce-service/carts/{memberId}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cartItemId))
                .andExpect(jsonPath("$[0].qty").value(3));

        mockMvc.perform(put("/commerce-service/carts/{cartItemId}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("qty", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qty").value(5));

        mockMvc.perform(delete("/commerce-service/carts/{cartItemId}", cartItemId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/commerce-service/carts/{memberId}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(kafkaTemplate, times(2))
                .send(eq("cart-added"), eq(memberId.toString()), any(GenericRecord.class));
    }

    @Test
    void rejectsInvalidQuantity() throws Exception {
        mockMvc.perform(post("/commerce-service/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", memberId, "productId", 10, "qty", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void returnsNotFoundForMissingCartItem() throws Exception {
        mockMvc.perform(put("/commerce-service/carts/{cartItemId}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("qty", 2))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
