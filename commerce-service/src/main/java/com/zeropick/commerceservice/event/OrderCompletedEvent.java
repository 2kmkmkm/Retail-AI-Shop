package com.zeropick.commerceservice.event;

import java.time.Instant;

public record OrderCompletedEvent(
        Long memberId,
        Long productId,
        String category,
        Integer qty,
        Long unitPrice,
        String orderNo,
        String paymentMethod,
        Instant occurredAt
) {
}
