package com.zeropick.commerceservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record BehaviorCreateRequest(
        @NotNull Long memberId,
        @NotNull Long productId,
        @NotNull BehaviorEventType eventType,
        @NotBlank String category,
        Instant occurredAt
) {
    public enum BehaviorEventType {
        PRODUCT_VIEWED
    }
}
