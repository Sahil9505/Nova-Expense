package com.nova.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Structured error body returned inside {@link com.nova.common.api.ApiResponse#data()}
 * when a request fails.
 */
public record ApiError(String code, String message, Instant timestamp, List<ApiError.FieldError> fields) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(ErrorCode code, String message) {
        return new ApiError(code.getCode(), message, Instant.now(), List.of());
    }

    public static ApiError of(ErrorCode code, String message, List<FieldError> fields) {
        return new ApiError(code.getCode(), message, Instant.now(), fields);
    }
}
