package com.zeropick.commerceservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OrderPayRequest(
        @NotNull
        @Pattern(regexp = "신용카드|카카오페이|토스페이|무통장입금")
        String paymentMethod
) {
}
