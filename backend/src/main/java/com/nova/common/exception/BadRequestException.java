package com.nova.common.exception;

/**
 * Thrown when a request is syntactically valid but semantically rejected.
 */
public class BadRequestException extends NovaException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}
