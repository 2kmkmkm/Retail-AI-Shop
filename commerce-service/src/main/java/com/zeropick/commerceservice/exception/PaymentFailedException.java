package com.zeropick.commerceservice.exception;

public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(Long orderId, Long productId, Throwable cause) {
        super("재고 차감에 실패하여 주문이 취소되었습니다: orderId="
                + orderId + ", productId=" + productId, cause);
    }
}
