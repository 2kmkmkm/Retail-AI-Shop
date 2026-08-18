package com.zeropick.commerceservice.event;

import java.time.Instant;

public record CartAddedEvent(
        Long memberId,
        Long productId,
        String category,
        Integer qty,
        Instant occurredAt
) {
}
