package com.zeropick.commerceservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartAddRequest(
        @NotNull Long memberId,
        @NotNull Long productId,
        @NotNull @Min(1) Integer qty
) {
}
