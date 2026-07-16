package com.nova.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable, documented error codes returned by the Nova API.
 * Codes are string-based so they survive refactoring and are safe to surface to clients.
 */
public enum ErrorCode {

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "One or more fields are invalid."),
    CONFLICT(HttpStatus.CONFLICT, "The request conflicts with the current state."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "The request could not be processed."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You are not allowed to perform this action."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String getCode() {
        return name();
    }
}
