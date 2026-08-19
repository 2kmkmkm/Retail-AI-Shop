package com.zeropick.commerceservice.exception;

public class OrderCancellationFailedException extends RuntimeException {

    public OrderCancellationFailedException(Long orderId, Long productId, Throwable cause) {
        super("재고 복구에 실패하여 주문을 취소하지 못했습니다: orderId="
                + orderId + ", productId=" + productId, cause);
    }
}
