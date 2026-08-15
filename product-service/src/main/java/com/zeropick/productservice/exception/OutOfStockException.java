package com.zeropick.productservice.exception;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(Long productId) {
        super("재고 부족: productId=" + productId);
    }
}
