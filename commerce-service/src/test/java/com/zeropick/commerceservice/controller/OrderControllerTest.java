package com.zeropick.commerceservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeropick.commerceservice.client.ProductCatalogService;
import com.zeropick.commerceservice.client.ProductStockService;
import com.zeropick.commerceservice.entity.Member;
import com.zeropick.commerceservice.entity.Order;
import com.zeropick.commerceservice.entity.OrderStatus;
import com.zeropick.commerceservice.exception.ProductNotFoundException;
import com.zeropick.commerceservice.exception.StockDeductionFailedException;
import com.zeropick.commerceservice.exception.StockRestoreFailedException;
import com.zeropick.commerceservice.repository.MemberRepository;
import com.zeropick.commerceservice.repository.OrderRepository;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductCatalogService productCatalogService;

    @MockitoBean
    private ProductStockService productStockService;

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

    @Test
    void paysPendingOrderAndPublishesCompletedEventsPerItem() throws Exception {
        long orderId = createOrder();
        when(productStockService.deduct(10L, 2)).thenReturn("간식/디저트");
        when(productStockService.deduct(20L, 3)).thenReturn("음료");

        mockMvc.perform(post("/commerce-service/orders/{orderId}/pay", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("paymentMethod", "카카오페이"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("카카오페이"));

        verify(productStockService).deduct(10L, 2);
        verify(productStockService).deduct(20L, 3);
        ArgumentCaptor<GenericRecord> eventCaptor = ArgumentCaptor.forClass(GenericRecord.class);
        verify(kafkaTemplate, times(2))
                .send(eq("order-completed"), eq(memberId.toString()), eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(record -> record.get("productId"))
                .containsExactly(10L, 20L);
        assertThat(eventCaptor.getAllValues())
                .allSatisfy(record -> assertThat(record.get("paymentMethod").toString())
                        .isEqualTo("카카오페이"));

        Order paidOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paidOrder.getPaymentMethod()).isEqualTo("카카오페이");
        assertThat(paidOrder.getPaidAt()).isNotNull();
    }

    @Test
    void cancelsOrderAndRestoresPreviouslyDeductedStockWhenDeductionFails() throws Exception {
        long orderId = createOrder();
        when(productStockService.deduct(10L, 2)).thenReturn("간식/디저트");
        when(productStockService.deduct(20L, 3))
                .thenThrow(new StockDeductionFailedException(20L));

        mockMvc.perform(post("/commerce-service/orders/{orderId}/pay", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("paymentMethod", "신용카드"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"));

        verify(productStockService).restore(10L, 2);
        verify(productStockService, never()).restore(20L, 3);
        verify(kafkaTemplate, never())
                .send(eq("order-completed"), any(String.class), any(GenericRecord.class));

        Order cancelledOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.getPaymentMethod()).isNull();
    }

    @Test
    void rejectsSecondPaymentBeforeDeductingStockAgain() throws Exception {
        long orderId = createOrder();
        when(productStockService.deduct(10L, 2)).thenReturn("간식/디저트");
        when(productStockService.deduct(20L, 3)).thenReturn("음료");
        String paymentBody = json(Map.of("paymentMethod", "토스페이"));

        mockMvc.perform(post("/commerce-service/orders/{orderId}/pay", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk());
        clearInvocations(productStockService, kafkaTemplate);

        mockMvc.perform(post("/commerce-service/orders/{orderId}/pay", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));

        verifyNoInteractions(productStockService);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void validatesPaymentMethodAndMissingOrder() throws Exception {
        long orderId = createOrder();

        mockMvc.perform(post("/commerce-service/orders/{orderId}/pay", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("paymentMethod", "카드"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/commerce-service/orders/{orderId}/pay", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("paymentMethod", "무통장입금"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        verifyNoInteractions(productStockService);
    }

    @Test
    void cancelsPendingOrderWithoutRestoringStock() throws Exception {
        long orderId = createOrder();

        mockMvc.perform(post("/commerce-service/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verifyNoInteractions(productStockService);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelsPaidOrderAndRestoresEveryItemStock() throws Exception {
        long orderId = createOrder();
        payOrder(orderId, "카카오페이");
        clearInvocations(productStockService, kafkaTemplate);

        mockMvc.perform(post("/commerce-service/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentMethod").value("카카오페이"));

        verify(productStockService).restore(10L, 2);
        verify(productStockService).restore(20L, 3);
        verify(productStockService, never()).deduct(any(Long.class), any(Integer.class));

        Order cancelledOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.getPaymentMethod()).isEqualTo("카카오페이");
    }

    @Test
    void rejectsRepeatedCancellationAndMissingOrder() throws Exception {
        long orderId = createOrder();
        mockMvc.perform(post("/commerce-service/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/commerce-service/orders/{orderId}/cancel", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));

        mockMvc.perform(post("/commerce-service/orders/{orderId}/cancel", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        verifyNoInteractions(productStockService);
    }

    @Test
    void keepsPaidOrderAndRedeductsRestoredStockWhenCancellationFails() throws Exception {
        long orderId = createOrder();
        payOrder(orderId, "신용카드");
        clearInvocations(productStockService, kafkaTemplate);
        when(productStockService.deduct(10L, 2)).thenReturn("간식/디저트");
        doThrow(new StockRestoreFailedException(20L, new RuntimeException("restore failed")))
                .when(productStockService).restore(20L, 3);

        mockMvc.perform(post("/commerce-service/orders/{orderId}/cancel", orderId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ORDER_CANCELLATION_FAILED"));

        verify(productStockService).restore(10L, 2);
        verify(productStockService).restore(20L, 3);
        verify(productStockService).deduct(10L, 2);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAID);
    }

    private long createOrder() throws Exception {
        String response = mockMvc.perform(post("/commerce-service/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d,
                                  "items": [
                                    {"productId": 10, "qty": 2},
                                    {"productId": 20, "qty": 3}
                                  ]
                                }
                                """.formatted(memberId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }

    private void payOrder(long orderId, String paymentMethod) throws Exception {
        when(productStockService.deduct(10L, 2)).thenReturn("간식/디저트");
        when(productStockService.deduct(20L, 3)).thenReturn("음료");
        mockMvc.perform(post("/commerce-service/orders/{orderId}/pay", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("paymentMethod", paymentMethod))))
                .andExpect(status().isOk());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
