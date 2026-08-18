package com.zeropick.commerceservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderCreateRequest(
        @NotNull @Positive Long memberId,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotNull @Positive Long productId,
            @NotNull @Min(1) Integer qty
    ) {
    }
}
