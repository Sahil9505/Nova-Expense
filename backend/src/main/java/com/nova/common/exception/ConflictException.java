package com.nova.common.exception;

/**
 * Thrown when a request conflicts with existing state (e.g. duplicate unique value).
 */
public class ConflictException extends NovaException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
