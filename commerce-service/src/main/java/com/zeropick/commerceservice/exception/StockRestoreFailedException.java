package com.zeropick.commerceservice.exception;

public class StockRestoreFailedException extends RuntimeException {

    public StockRestoreFailedException(Long productId, Throwable cause) {
        super("상품 재고를 복구할 수 없습니다: " + productId, cause);
    }
}
