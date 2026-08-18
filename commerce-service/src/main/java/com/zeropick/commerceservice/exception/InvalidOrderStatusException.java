package com.zeropick.commerceservice.exception;

import com.zeropick.commerceservice.entity.OrderStatus;

public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(Long orderId, OrderStatus status) {
        super("처리할 수 없는 주문 상태입니다: orderId=" + orderId + ", status=" + status);
    }
}
