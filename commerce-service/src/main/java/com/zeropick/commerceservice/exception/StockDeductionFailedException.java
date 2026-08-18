package com.zeropick.commerceservice.exception;

public class StockDeductionFailedException extends RuntimeException {

    private final Long productId;

    public StockDeductionFailedException(Long productId) {
        this(productId, null);
    }

    public StockDeductionFailedException(Long productId, Throwable cause) {
        super("상품 재고를 차감할 수 없습니다: " + productId, cause);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
