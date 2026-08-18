package com.zeropick.commerceservice.controller;

import com.zeropick.commerceservice.client.ProductCatalogService;
import com.zeropick.commerceservice.entity.Member;
import com.zeropick.commerceservice.entity.Order;
import com.zeropick.commerceservice.entity.OrderStatus;
import com.zeropick.commerceservice.exception.ProductNotFoundException;
import com.zeropick.commerceservice.repository.MemberRepository;
import com.zeropick.commerceservice.repository.OrderRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductCatalogService productCatalogService;

    @MockitoBean
    private KafkaTemplate<String, GenericRecord> kafkaTemplate;

    private Long memberId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        memberRepository.deleteAll();
        Member member = memberRepository.save(Member.builder()
                .email("order@example.com")
                .password("encoded-password")
                .name("주문 테스트")
                .build());
        memberId = member.getId();

        when(productCatalogService.getProduct(10L)).thenReturn(
                new ProductCatalogService.CatalogProduct(10L, "제로 초콜릿", 3_500L, "간식/디저트")
        );
        when(productCatalogService.getProduct(20L)).thenReturn(
                new ProductCatalogService.CatalogProduct(20L, "제로 탄산", 2_000L, "음료")
        );
    }

    @Test
    void createsPendingOrderFromCatalogSnapshots() throws Exception {
        mockMvc.perform(post("/commerce-service/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d,
                                  "items": [
                                    {"productId": 10, "qty": 2},
                                    {"productId": 20, "qty": 3}
                                  ],
                                  "totalPrice": 1,
                                  "name": "클라이언트 위조값"
                                }
                                """.formatted(memberId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.orderNo").value(org.hamcrest.Matchers.matchesPattern("ZP\\d+")))
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.totalPrice").value(13_000))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").doesNotExist())
                .andExpect(jsonPath("$.orderedAt").isNotEmpty())
                .andExpect(jsonPath("$.items[0].productName").value("제로 초콜릿"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(3_500))
                .andExpect(jsonPath("$.items[1].productName").value("제로 탄산"));

        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orders.get(0).getPaymentMethod()).isNull();
        assertThat(orders.get(0).getTotalPrice()).isEqualTo(13_000L);
    }

    @Test
    void rejectsEmptyItemsAndInvalidQuantity() throws Exception {
        mockMvc.perform(post("/commerce-service/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d, "items": []}
                                """.formatted(memberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/commerce-service/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d, "items": [{"productId": 10, "qty": 0}]}
                                """.formatted(memberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void returnsNotFoundAndDoesNotSavePartialOrder() throws Exception {
        when(productCatalogService.getProduct(999L)).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(post("/commerce-service/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d,
                                  "items": [
                                    {"productId": 10, "qty": 1},
                                    {"productId": 999, "qty": 1}
                                  ]
                                }
                                """.formatted(memberId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void returnsNotFoundForMissingMember() throws Exception {
        mockMvc.perform(post("/commerce-service/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": 999999, "items": [{"productId": 10, "qty": 1}]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }
}
