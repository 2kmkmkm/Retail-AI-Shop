package com.zeropick.commerceservice.exception;

public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(Throwable cause) {
        super("상품 서비스에 연결할 수 없습니다.", cause);
    }
}
