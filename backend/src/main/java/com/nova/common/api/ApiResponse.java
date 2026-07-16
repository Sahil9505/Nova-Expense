package com.nova.common.api;

import java.time.Instant;

/**
 * Consistent envelope returned by every Nova API response.
 *
 * @param success   whether the request was handled successfully
 * @param message   human-readable summary of the outcome
 * @param data      payload for successful responses, or an error detail object
 * @param timestamp server time the response was produced
 */
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, Instant.now());
    }
}
