package com.zeropick.commerceservice.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        Map<String, String> errors
) {
    public static ApiErrorResponse of(int status, String code, String message) {
        return new ApiErrorResponse(LocalDateTime.now(), status, code, message, Map.of());
    }

    public static ApiErrorResponse of(int status, String code, String message, Map<String, String> errors) {
        return new ApiErrorResponse(LocalDateTime.now(), status, code, message, errors);
    }
}
