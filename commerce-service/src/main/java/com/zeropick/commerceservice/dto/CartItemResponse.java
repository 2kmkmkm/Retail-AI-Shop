package com.zeropick.commerceservice.dto;

import com.zeropick.commerceservice.entity.CartItem;

public record CartItemResponse(Long id, Long memberId, Long productId, Integer qty) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getMember().getId(),
                item.getProductId(),
                item.getQty()
        );
    }
}
