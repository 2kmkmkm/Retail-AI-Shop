package com.zeropick.commerceservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateEmailException exception) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(), "DUPLICATE_EMAIL", exception.getMessage()
        ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(), "INVALID_CREDENTIALS", exception.getMessage()
        ));
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberNotFound(MemberNotFoundException exception) {
        return notFound("MEMBER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCartItemNotFound(CartItemNotFoundException exception) {
        return notFound("CART_ITEM_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(OrderNotFoundException exception) {
        return notFound("ORDER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderStatus(InvalidOrderStatusException exception) {
        return conflict("INVALID_ORDER_STATUS", exception.getMessage());
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentFailed(PaymentFailedException exception) {
        return conflict("PAYMENT_FAILED", exception.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException exception) {
        return notFound("PRODUCT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ProductServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleProductServiceUnavailable(ProductServiceUnavailableException exception) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(), "PRODUCT_SERVICE_UNAVAILABLE", exception.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                status.value(), "INVALID_REQUEST", "요청 값이 올바르지 않습니다.", errors
        ));
    }

    private ResponseEntity<ApiErrorResponse> notFound(String code, String message) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(), code, message
        ));
    }

    private ResponseEntity<ApiErrorResponse> conflict(String code, String message) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(), code, message
        ));
    }
}
