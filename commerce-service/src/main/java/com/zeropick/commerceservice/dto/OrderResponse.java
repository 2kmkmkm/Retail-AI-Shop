package com.zeropick.commerceservice.dto;

import com.zeropick.commerceservice.entity.Order;
import com.zeropick.commerceservice.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNo,
        Long memberId,
        Long totalPrice,
        OrderStatus status,
        String paymentMethod,
        LocalDateTime orderedAt,
        List<Item> items
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getMember().getId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getOrderedAt(),
                order.getItems().stream()
                        .map(item -> new Item(
                                item.getProductId(),
                                item.getProductName(),
                                item.getQty(),
                                item.getUnitPrice()
                        ))
                        .toList()
        );
    }

    public record Item(Long productId, String productName, Integer qty, Long unitPrice) {
    }
}
