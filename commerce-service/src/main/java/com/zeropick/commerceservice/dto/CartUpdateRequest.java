package com.zeropick.commerceservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartUpdateRequest(@NotNull @Min(1) Integer qty) {
}
