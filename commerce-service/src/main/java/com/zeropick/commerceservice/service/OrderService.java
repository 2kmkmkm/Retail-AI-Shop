package com.zeropick.commerceservice.service;

import com.zeropick.commerceservice.client.ProductCatalogService;
import com.zeropick.commerceservice.client.ProductStockService;
import com.zeropick.commerceservice.dto.OrderCreateRequest;
import com.zeropick.commerceservice.dto.OrderPayRequest;
import com.zeropick.commerceservice.dto.OrderResponse;
import com.zeropick.commerceservice.entity.Member;
import com.zeropick.commerceservice.entity.Order;
import com.zeropick.commerceservice.entity.OrderItem;
import com.zeropick.commerceservice.exception.MemberNotFoundException;
import com.zeropick.commerceservice.exception.OrderNotFoundException;
import com.zeropick.commerceservice.exception.PaymentFailedException;
import com.zeropick.commerceservice.exception.StockDeductionFailedException;
import com.zeropick.commerceservice.event.OrderCompletedEvent;
import com.zeropick.commerceservice.repository.MemberRepository;
import com.zeropick.commerceservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final long ORDER_NUMBER_OFFSET = 1000L;

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductCatalogService productCatalogService;
    private final ProductStockService productStockService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new MemberNotFoundException(request.memberId()));

        Order order = Order.builder()
                .orderNo(temporaryOrderNo())
                .member(member)
                .totalPrice(0L)
                .build();

        long totalPrice = 0L;
        for (OrderCreateRequest.Item requestedItem : request.items()) {
            ProductCatalogService.CatalogProduct product =
                    productCatalogService.getProduct(requestedItem.productId());
            totalPrice = Math.addExact(
                    totalPrice,
                    Math.multiplyExact(product.price(), requestedItem.qty().longValue())
            );
            order.addItem(OrderItem.builder()
                    .productId(product.id())
                    .productName(product.name())
                    .qty(requestedItem.qty())
                    .unitPrice(product.price())
                    .build());
        }

        order.changeTotalPrice(totalPrice);
        Order saved = orderRepository.saveAndFlush(order);
        saved.assignOrderNo("ZP" + (ORDER_NUMBER_OFFSET + saved.getId()));
        orderRepository.flush();
        return OrderResponse.from(saved);
    }

    @Transactional(noRollbackFor = PaymentFailedException.class)
    public OrderResponse pay(Long orderId, OrderPayRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.validatePayable();

        List<DeductedStock> deductedStocks = new ArrayList<>();
        try {
            for (OrderItem item : order.getItems()) {
                String category = productStockService.deduct(item.getProductId(), item.getQty());
                deductedStocks.add(new DeductedStock(item, category));
            }
        } catch (StockDeductionFailedException exception) {
            restoreDeductedStocks(deductedStocks);
            order.cancel();
            throw new PaymentFailedException(orderId, exception.getProductId(), exception);
        }

        order.markPaid(request.paymentMethod());
        Instant occurredAt = Instant.now();
        for (DeductedStock deductedStock : deductedStocks) {
            OrderItem item = deductedStock.item();
            eventPublisher.publishEvent(new OrderCompletedEvent(
                    order.getMember().getId(),
                    item.getProductId(),
                    deductedStock.category(),
                    item.getQty(),
                    item.getUnitPrice(),
                    order.getOrderNo(),
                    request.paymentMethod(),
                    occurredAt
            ));
        }
        return OrderResponse.from(order);
    }

    private void restoreDeductedStocks(List<DeductedStock> deductedStocks) {
        for (int index = deductedStocks.size() - 1; index >= 0; index--) {
            OrderItem item = deductedStocks.get(index).item();
            try {
                productStockService.restore(item.getProductId(), item.getQty());
            } catch (RuntimeException exception) {
                log.error("결제 실패 보상 중 재고 복구에 실패했습니다: productId={}, qty={}",
                        item.getProductId(), item.getQty(), exception);
            }
        }
    }

    private String temporaryOrderNo() {
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record DeductedStock(OrderItem item, String category) {
    }
}
