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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "The presented token is invalid or expired."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You are not allowed to perform this action."),
    RECEIPT_UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "That file type is not supported."),
    RECEIPT_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "The file exceeds the maximum allowed size."),
    RECEIPT_INVALID_IMAGE(HttpStatus.BAD_REQUEST, "The image could not be read; it may be corrupted."),
    RECEIPT_OCR_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The receipt scanner is not available right now."),
    RECEIPT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "We could not process this receipt."),
    RECEIPT_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "We could not store this receipt."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The AI assistant is temporarily unavailable. Please try again in a moment."),
    AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "The AI assistant took too long to respond. Please try again."),
    AI_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "The AI assistant is busy right now. Please try again shortly."),
    AI_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "The AI assistant returned an unusable response. Please try rephrasing."),
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
