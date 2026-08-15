package com.zeropick.productservice.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long productId) {
        super("상품 없음: productId=" + productId);
    }
}
