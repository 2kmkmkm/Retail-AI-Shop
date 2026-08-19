package com.zeropick.commerceservice.event;

import java.time.Instant;

public record ProductViewedEvent(
        Long memberId,
        Long productId,
        String category,
        Instant occurredAt
) {
}
